package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
class MethodSpec {
    // https://swagger.io/docs/specification/describing-parameters/#path-parameters
    //if provided, only parameters in this list will be allowed
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<ParameterSpec> parameters;


    // gives OpenAPI spec compatible structure:
    //   - pros of some interoperatibility, ease of building
    //   - cons of having it more brittle, more verbose; much harder to write from scratch/
    //     reverse engineer from API
    //
    //
    // responses:
    //   200:
    //     description: 'A list of users'
    //     content:
    //        application/json:
    //          schema:
    //          $ref: '#/components/schemas/ArrayOfUsers'

    //

    Map<Integer, ResponseSpec> responses;

    //alternatively, we could just put `schema` here; by default filter any 2xx response by it and
    // pass back the result.

    //


    //q: what about errors? it seems too aggressive to enumerate ALL error types here and filter
    //   error response that don't validate against them.
    // - risk is if error provides any kind of information reveal, such as a backdoor lookup;
    //   eg call users/p~asdfasdfasdf/mail and get a 403 'no mailbox for alice@worklytics.co'


}
