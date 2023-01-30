package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
class ResponseSpec {
    String description;

    Map<String, JsonSchema> content;

}
