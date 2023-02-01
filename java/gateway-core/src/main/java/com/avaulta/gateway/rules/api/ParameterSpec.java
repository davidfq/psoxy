package com.avaulta.gateway.rules.api;

import com.avaulta.gateway.rules.jsonschema.JsonSchema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Value
public class ParameterSpec {

    String name;

    // enum of  'path', 'query'
    //@NonNull
    //Type in;

    //q: support this?
    //Boolean required;

    @JsonProperty("$ref")
    String ref;

    //q: potentially this should be a real standard-compliant JSON schema, validated using a 3rd-party
    // library; whereas filter cases are not?
    JsonSchema schema;

//    public enum Type {
//        PATH,
//        QUERY,
//        //q: add 'header'? do we have a data source that expects this??
//        ;
//
//        @JsonValue
//        String getJsonValue() {
//            return name().toLowerCase();
//        }
//    }
}
