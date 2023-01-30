package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

// use this??
// https://github.com/swagger-api/swagger-parser
// produces https://github.com/swagger-api/swagger-core/blob/master/modules/swagger-models/src/main/java/io/swagger/v3/oas/models/OpenAPI.java#L36
//  - lots of stuff we don't need in there, but may give out-of-the box validation ...
//    - we don't want response validation, but we do want *request* validation
//  - drawback is that if we deserialize from OpenAPI spec, so we have to support more of it than
//    we need?

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointSpec {

    @JsonIgnore // when this is serialized to JSON/YAML, this should be the key, with this class being the value
    String pathSpec;

    MethodSpec head;

    MethodSpec get;

    //q: why support stuff beyond get? extensibility to the (rare) cases where we use some other
    // methods and a future use-case of provisioning webhooks/etc

    MethodSpec post;

    MethodSpec put;

    MethodSpec delete;

    //String summary;

    //String description;


}
