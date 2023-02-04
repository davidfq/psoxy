package com.avaulta.gateway.rules.api;

import com.avaulta.gateway.rules.RuleSet;
import com.avaulta.gateway.rules.jsonschema.JsonSchema;
import com.avaulta.gateway.rules.jsonschema.RefEnvironment;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

// if we used full OpenAPI Spec here, would give (nice?) property that copy-paste of an API's
// OpenAPI spec would be a valid rule set that would "lock" a view of the API to that schema.
// one *could* add transforms to the rules, remove paths, etc.
@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RESTRules implements RuleSet {

    Map<String, EndpointSpec> paths;

    @Builder.Default
    Components components = Components.builder().build();


    @Deprecated
    String defaultScopeIdForSource;

    @Builder
    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Components implements RefEnvironment {

        Map<String, ParameterSpec> parameters;
        Map<String, JsonSchema> schemas;


        public ParameterSpec resolveParameter(ParameterSpec parameterSpec) {
            if (parameterSpec.getRef() == null) {
                return parameterSpec;
            } else {
                return parameters.get(parameterSpec.getRef());
            }
        }

        public ParameterSpec resolveParameter(String ref) {
            if (ref.startsWith("#/parameters/")) {
                //weird??
                // we lose parameter name!?!?
                return parameters.get(ref.substring("#/parameters/".length()));
            } else {
                throw new IllegalArgumentException("don't know how to resolve ref: " + ref);
            }
        }

        @Override
        public JsonSchema resolve(String ref) {
            if (ref.startsWith("#/schemas/")) {
                return schemas.get(ref.substring("#/schemas/".length()));
            } else {
                throw new IllegalArgumentException("don't know how to resolve ref: " + ref);
            }
        }
    }


}
