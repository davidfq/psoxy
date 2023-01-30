package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RESTRules {

    Map<String, EndpointSpec> paths;


    @Builder
    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Components {


        Map<String, ParameterSpec> parameters;
        Map<String, JsonSchema> schemas;
    }
}
