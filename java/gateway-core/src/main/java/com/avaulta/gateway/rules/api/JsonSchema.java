package com.avaulta.gateway.rules.api;

import com.fasterxml.jackson.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Objects;

@Builder
@NoArgsConstructor
@AllArgsConstructor // for builder
@Data
@JsonPropertyOrder({"$schema"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"title"})
public class JsonSchema {

    //q: should we drop this? only makes sense at root of schema
    @JsonProperty("$schema")
    String schema;

    String type;

    @JsonProperty("$ref")
    String ref;

    //only applicable if type==object
    Boolean additionalProperties;


    //only applicable if type==String
    // what are formats?
    // https://swagger.io/docs/specification/data-models/data-types/#string
    // use this to mark 'email', 'email-header', 'user-id' ?
    //   - what about fields that are polymorphic ids, but *may* be user ids?
    //   - since 'email', 'user-id' likely non-standard, this approach means grabbing source's
    //     won't be a great starting point

    //   other identifiers?

    //  'encrypted-pseudonym'?

    // could work with a policy-based approach?
    //   - except whether we want 'encrypt' or 'pseudonymize' transform depends on use-case, not
    //    just data type/format.  whether 'encrypt' is allowed is a question for the ruleset.


    // build this stuff by annotating with https://github.com/swagger-api/swagger-core/tree/master/modules/swagger-annotations/src/main/java/io/swagger/v3/oas/annotations ??

    String format;

    Map<String, JsonSchema> properties;

    //only applicable if type==array
    JsonSchema items;

    // @JsonProperties("$defs") on property, lombok getter/setter don't seem to do the right thing
    // get java.lang.IllegalArgumentException: Unrecognized field "definitions" (class com.avaulta.gateway.rules.SchemaRuleUtils$JsonSchema)
    //        //  when calling objectMapper.convertValue(((JsonNode) schema), JsonSchema.class);??
    // perhaps it's a problem with the library we use to build the JsonNode schema??
    Map<String, JsonSchema> definitions;

    // part of JSON schema standard, but how to support for filters?
    //  what if something validates against multiple of these, but filtering by the valid ones
    //  yields different result??
    // use case would be polymorphism, such as a groupMembers array can can contain
    // objects of type Group or User, to provide hierarchical groups
    // --> take whichever schema produces the "largest" result (eg, longest as a string??)
    //List<CompoundJsonSchema> anyOf;

    // part of JSON schema standard, but how to support for filters?
    //  what if something validates against multiple of these, but filtering by the valid ones
    //  yields different result??
    // ultimately, don't see a use case anyways
    //List<CompoundJsonSchema> oneOf;

    // part of JSON schema standard
    // it's clear how we would implement this as a filter (chain them), but not why
    // this would ever be a good use case
    //List<CompoundJsonSchema> allOf;

    //part of JSON schema standard, but not a proxy-filtering use case this
    // -- omitting the property produces same result
    // -- no reason you'd ever want to all objects that DON'T match a schema, right?
    // -- only use case I think of is to explicitly note what properties we know are in
    //   source schema, so intend for filter to remove (rather than filter removing them by
    //   omission)
    //CompoundJsonSchema not;


    @JsonIgnore
    public boolean isRef() {
        return ref != null;
    }

    @JsonIgnore
    public boolean isString() {
        return Objects.equals(type, "string");
    }

    @JsonIgnore
    public boolean isNumber() {
        return Objects.equals(type, "number");
    }

    @JsonIgnore
    public boolean isInteger() {
        return Objects.equals(type, "integer");
    }

    @JsonIgnore
    public boolean isObject() {
        return Objects.equals(type, "object");
    }

    @JsonIgnore
    public boolean isArray() {
        return Objects.equals(type, "array");
    }

    @JsonIgnore
    public boolean isBoolean() {
        return Objects.equals(type, "boolean");
    }

    @JsonIgnore
    public boolean isNull() {
        return Objects.equals(type, "null");
    }

    @JsonIgnore
    public boolean hasType() {
        return this.type != null;
    }
}
