package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class ParameterSpec {

    // enum of  'path', 'query'
    @NonNull
    Type in;

    Boolean required;

    JsonSchema schema;

    enum Type {
        PATH,
        QUERY,
        //q: add 'header'? do we have a data source that expects this??
        ;

        @JsonValue
        String getJsonValue() {
            return name().toLowerCase();
        }
    }
}
