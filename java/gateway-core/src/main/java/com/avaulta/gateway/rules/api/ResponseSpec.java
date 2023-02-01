package com.avaulta.gateway.rules.api;

import com.avaulta.gateway.rules.jsonschema.JsonSchema;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseSpec {
    String description;


    // content-type ('application/json') --> JSON schema
    // in effect, this is really our proprietary "JSON schema filter"
    Map<String, JsonSchema> content;


}
