package com.avaulta.gateway.rules;

import java.net.http.HttpRequest;
import java.util.Map;

public interface RequestValidator {

    boolean isAllowed(HttpRequest request);

    interface HttpRequest {
        String getMethod();
        String getPath();
        Map<String, String> getHeaders();
        Map<String, String> getQueryParams();
        String getBody();
    }
}
