package com.avaulta.gateway.rules;

import com.avaulta.gateway.rules.api.JsonSchema;
import com.avaulta.gateway.rules.transforms.Transform;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@JsonPropertyOrder({"pathRegex", "allowedQueryParams", "transforms"})
@Builder(toBuilder = true)
@AllArgsConstructor //for builder
@NoArgsConstructor //for Jackson
@Getter
public class Endpoint {

    String pathRegex;

    @Deprecated // will be removed in v0.5; use
    //if provided, only query params in this list will be allowed
    @JsonInclude(JsonInclude.Include.NON_NULL)
    List<String> allowedQueryParams;

    //TODO: add conditionally allowed query parameters? (eg, match value against a regex?)

    @JsonIgnore
    public Optional<List<String>> getAllowedQueryParamsOptional() {
        return Optional.ofNullable(allowedQueryParams);
    }


    //NOTE: in OpenAPI Spec, this is in the structure (eg endpoint --> method --> parameters,schema)
    //if provided, only http methods in this list will be allowed
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Set<String> allowedMethods;

    @JsonIgnore
    public Optional<Set<String>> getAllowedMethods() {
        return Optional.ofNullable(allowedMethods);
    }



    JsonSchema responseSchema;

    @JsonIgnore
    public Optional<JsonSchema> getResponseSchemaOptional() {
        return Optional.ofNullable(responseSchema);
    }



    @Setter
    @JsonInclude(value = JsonInclude.Include.NON_EMPTY)
    @Singular
    List<Transform> transforms = new ArrayList<>();

    @Override
    public Endpoint clone() {
        return this.toBuilder()
            .clearTransforms()
            .transforms(this.transforms.stream().map(Transform::clone).collect(Collectors.toList()))
            .allowedQueryParams(this.getAllowedQueryParamsOptional().map(ArrayList::new).orElse(null))
            .build();
    }


}