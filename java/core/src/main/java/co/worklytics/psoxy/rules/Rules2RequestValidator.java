package co.worklytics.psoxy.rules;

import co.worklytics.psoxy.utils.URLUtils;
import com.avaulta.gateway.rules.Endpoint;
import com.avaulta.gateway.rules.RequestValidator;
import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;
import lombok.NonNull;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class Rules2RequestValidator implements RequestValidator {

    @NonNull Rules2 rules;

    @AssistedInject
    public Rules2RequestValidator(@Assisted Rules2 rules) {
        this.rules = rules;
    }

    private final Object $writeLock = new Object[0];
    transient Map<Endpoint, Pattern> compiledAllowedEndpoints;

    @Override
    public boolean isAllowed(HttpRequest request) {
        if (rules.getAllowAllEndpoints()) {
            return true;
        } else {
            String queryString =
                request.getQueryParams().isEmpty() ? "" : "?" + URLUtils.asQueryString(request.getQueryParams());

            return getCompiledAllowedEndpoints().entrySet().stream()
                .filter(entry -> entry.getValue().matcher(request.getPath() + queryString).matches())
                .filter(entry -> entry.getKey().getAllowedMethods()
                    .map(methods -> methods.stream().map(String::toUpperCase).collect(Collectors.toList())
                        .contains(request.getMethod()))
                    .orElse(true))
                .filter(entry -> entry.getKey().getAllowedQueryParamsOptional()
                    .map(allowedParams -> allowedParams.containsAll(request.getQueryParams().keySet()))
                    .orElse(true))
                .findAny().isPresent();
        }
    }

    Map<Endpoint, Pattern> getCompiledAllowedEndpoints() {
        if (compiledAllowedEndpoints == null) {
            synchronized ($writeLock) {
                compiledAllowedEndpoints = rules.getEndpoints().stream()
                    .collect(Collectors.toMap(Function.identity(),
                        endpoint -> Pattern.compile(endpoint.getPathRegex(), CASE_INSENSITIVE)));
            }
        }
        return compiledAllowedEndpoints;
    }

}
