package com.avaulta.gateway.rules;

import com.avaulta.gateway.rules.api.EndpointSpec;
import com.avaulta.gateway.rules.api.MethodSpec;
import com.avaulta.gateway.rules.api.RESTRules;
import com.avaulta.gateway.rules.jsonschema.JsonSchema;
import com.avaulta.gateway.rules.jsonschema.RefEnvironment;
import com.avaulta.gateway.rules.jsonschema.Validator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

import javax.inject.Inject;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RESTRulesRequestValidator implements RequestValidator {

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Validator validator;
    RESTRules rules;


    @AssistedInject
    public RESTRulesRequestValidator(@Assisted RESTRules rules) {
        this.rules = rules;
    }


    @Override
    public boolean isAllowed(HttpRequest request) {
        return matchMethod(request).isPresent();
    }

    //uses:
    // - validate request
    // - transform request?
    //    - schema defined in the rules may give more power to do this ...
    // - transform response

    // use https://github.com/openapi4j/openapi4j/blob/master/openapi-operation-validator/src/main/java/org/openapi4j/operation/validator/validation/RequestValidator.java#L132
    // to validate the request against the rules?

    /**
     * match request to method defined by rules, if any.
     * q: better to call this 'resolve'?
     *
     * q: better to use org.apache.http.HttpRequest here, rather than our own interface??
     *
     * @param request
     * @return
     */
    public Optional<MethodSpec> matchMethod(RequestValidator.HttpRequest request) {
        return rules.getPaths().entrySet().stream()
            .map(entry -> matchHttpMethod(entry.getValue(), request)
                .map(methodSpec -> Pair.of(entry.getKey(), methodSpec)).orElse(null))
            .filter(Objects::nonNull)
            .filter(entry -> matchPath(rules, entry.getKey(), entry.getValue(), request)) //somehow log endpoints that aren't matched due to path
            .filter(entry -> matchQuery(rules, entry.getValue(), request)) //somehow log endpoints that aren't matched due to query
            .map(Map.Entry::getValue)
            .findFirst();
    }

    // can we use something like this??
    // https://github.com/openapi4j/openapi4j/blob/master/openapi-operation-validator/src/main/java/org/openapi4j/operation/validator/util/convert/ParameterConverter.java

    boolean matchPath(RESTRules env, String pathTemplate, MethodSpec methodSpec, RequestValidator.HttpRequest request) {

        Pair<Pattern, List<String>> pattern = toPattern(pathTemplate);

        Matcher matcher = pattern.getLeft().matcher(request.getPath());
        if (matcher.matches()) {
            return pattern.getRight().stream().allMatch(paramName ->
                methodSpec.getPathParameters().stream()
                    .filter(parameterSpec -> Objects.equals(parameterSpec.getName(), paramName))
                    .filter(parameterSpec -> isUrlSequenceValid(
                        matcher.group(new ArrayList<>(pattern.getRight()).indexOf(paramName) + 1),
                        env.getComponents().resolveParameter(parameterSpec).getSchema(),
                        env.getComponents()))
                    .findAny().isPresent()
            );
        } else {
            return false;
        }
    }

    /**
     * convert pathTemplate to Pattern with capture groups to match against actual paths plus
     * names of path parameters
     *
     * @param pathTemplate
     *
     * @return (Pattern, List<String>) where the List<String> is the names of the path parameters
     */
    Pair<Pattern, List<String>> toPattern(String pathTemplate) {
        return Pair.of(Pattern.compile(pathTemplate.replaceAll(PATH_TEMPLATE_PARAM_REGEX, PATH_FRAGMENT_REGEX)),
            parseParams(pathTemplate));
    }

    final String PATH_TEMPLATE_PARAM_REGEX = "\\{([^\\}]+)\\}";
    final String PATH_FRAGMENT_REGEX = "([^/]+)";
    final Pattern PATH_TEMPLATE_PARAM_PATTERN = Pattern.compile(PATH_TEMPLATE_PARAM_REGEX);

    List<String> parseParams(String pathTemplate) {
        return PATH_TEMPLATE_PARAM_PATTERN.matcher(pathTemplate).results().map(m -> m.group(1)).collect(Collectors.toList());
    }

    boolean matchQuery(RESTRules env, MethodSpec endpoint, RequestValidator.HttpRequest request) {
        return endpoint.getQueryParameters() == null
            || request.getQueryParams().entrySet()
            .stream()
            .allMatch(entry -> endpoint.getQueryParameters().stream()
                .filter(p -> Objects.equals(p.getName(), entry.getKey()))
                .filter(p -> isUrlSequenceValid(
                    entry.getValue(),
                    env.getComponents().resolveParameter(p).getSchema(),
                    env.getComponents()))
                .findAny().isPresent());
    }

    Optional<MethodSpec> matchHttpMethod(EndpointSpec endpoint, RequestValidator.HttpRequest request) {
        String ucMethod = request.getMethod().toUpperCase();

        if (ucMethod.equals("GET")) {
            return Optional.ofNullable(endpoint.getGet());
        } else if (ucMethod.equals("POST")) {
            return Optional.ofNullable(endpoint.getPost());
        } else if (ucMethod.equals("PUT")) {
            return Optional.ofNullable(endpoint.getPut());
        } else if (ucMethod.equals("DELETE")) {
            return Optional.ofNullable(endpoint.getDelete());
        } else {
            return Optional.empty();
        }
    }


    /**
     *
     * @param sequence a URL-encoded value to validate
     * @param schema the schema to validate against
     * @param refEnvironment to resolve any $ref's in the schema
     * @return whether the sequence is valid per the schema
     */
    @SneakyThrows
    public boolean isUrlSequenceValid(@NonNull String sequence, JsonSchema schema, RefEnvironment refEnvironment) {
        String decoded = URLDecoder.decode(sequence, StandardCharsets.UTF_8);

        // another OpenAPI Spec validator implementation has a BUNCH parameter conversion cases that
        // this doesn't handle; but I can't find that these are any kind of "standard" way to
        // encode complex types in URL query/path parameters
        // https://github.com/openapi4j/openapi4j/blob/master/openapi-operation-validator/src/main/java/org/openapi4j/operation/validator/util/convert/ParameterConverter.java#L29-L39



        boolean valid = false;

        try {
            // this will let a JSON-encoded string be passed through and its deserialized
            // form validated against the schema, rather than it's string encoding validated against
            // the schema. Is this what we want?

            // to avoid this, we could JSON-escape before reading to tree? (eg, force interpretation
            // as a string rather than complex json type)

            valid = validator.isValid(objectMapper.readTree(decoded), schema, refEnvironment);
        } catch (JsonParseException e) {
            // if we can't parse the JSON, it's not valid
        }

        return valid
            || validator.isValid(objectMapper.readTree("\"" + decoded + "\""), schema, refEnvironment);
    }

}
