package com.avaulta.gateway.rules.jsonschema;

import com.avaulta.gateway.rules.transforms.Transform;
import com.fasterxml.jackson.annotation.*;
import lombok.*;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * subset of JSON Schema 2020-12, with transforms added and various features removed.
 *
 * In particular, we (plan to) support:
 *   - all 'string' features, including defined formats
 *   - basic string/numeric/boolean/null validation
 *   - 'object', including 'properties'
 *   - refs, but not remote refs
 *
 * Features are removed if:
 *   1. not clear how to interpret it as a "filter", or interpreting as a "filter" would produce
 *      result that is not valid per the same schema.
 *   2. not useful for the proxy use case
 *
 * Unsupported 'string' features:
 *   - length
 *   - pattern - TBD, but probably *will* support this (use case of validating
 *   - format - TBD, but probably will support a couple of these
 *
 * Unsupported 'object' features:
 *   - Pattern Properties: https://json-schema.org/understanding-json-schema/reference/object.html#pattern-properties
 *   - Required Properties: https://json-schema.org/understanding-json-schema/reference/object.html#required-properties
 *   - Property Names: https://json-schema.org/understanding-json-schema/reference/object.html#property-names
 *   - Object Size: https://json-schema.org/understanding-json-schema/reference/object.html#size
 *   - Unevaluated Properties: https://json-schema.org/understanding-json-schema/reference/object.html#unevaluated-properties
 *
 * Unsupported 'array' features:
 *   - Tuple validation: https://json-schema.org/understanding-json-schema/reference/array.html#tuple-validation
 *   - Contains
 *   - Length
 *   - Uniqueness
 *
 * Unsupported numeric features:
 *  - multipleOf
 *  - range constraints
 *
 * q: rather than explicit list of transforms, have them potentially be implied by 'format'?
 * q: rather than transforms with jsonPaths, have them always operate on the root, so in effect
 *    you would push them to the lowest level?
 *
 * q: for clarity, should we call this JsonSchemaFilter??
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor // for builder
@Data
@JsonPropertyOrder({"$schema"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties({"title"})
public class JsonSchema implements RefEnvironment {

    //q: should we drop this? only makes sense at root of schema
    @JsonProperty("$schema")
    String schema;

    String type;

    @JsonProperty("$ref")
    String ref;

    //only applicable if type==object
    Boolean additionalProperties;


    //avoid repeating logic to default to 'false' in BOTH Validate and Filter
    @JsonIgnore //don't want to clutter serialized schemas with this
    public Boolean getAdditionalPropertiesOrDefault() {
        return ObjectUtils.defaultIfNull(additionalProperties, false);
    }


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


    // first transform to apply, if any
    Transform transform;

    // transforms to imply, if any; (after 'transform')
    List<Transform> transforms;


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

    @Override
    public JsonSchema resolve(@NonNull String ref) {
        if (ref.equals("#")) {
            return this;
        } else if (ref.startsWith("#/definitions/")) {
            return definitions.get(ref.substring("#/definitions/".length()));
        } else {
            throw new IllegalArgumentException("don't know how to resolve ref: " + ref);
        }
    }
}
