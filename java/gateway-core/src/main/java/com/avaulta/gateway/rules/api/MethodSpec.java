package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Builder
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MethodSpec {




//    @JsonInclude(JsonInclude.Include.NON_NULL)
//    List<ParameterSpec> parameters;

    //clearer than parameters with `in: query` / `in: path`, but departs from OpenAPI spec
    // does OpenAPI spec have some other way to allow "any query parameters", "any headers", etc?
    // (any path parameters is non-sensical)
    List<ParameterSpec> pathParameters;
    List<ParameterSpec> queryParameters;

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
