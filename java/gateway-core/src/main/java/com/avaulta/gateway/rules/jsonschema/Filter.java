package com.avaulta.gateway.rules.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * filter JSON against a JSON schema, which we're defining as meaning that any node that isn't valid
 * according to the schema is removed from the JSON
 *
 *   - array items that filter to `null` are removed from the array
 *   - object properties that aren't in schema or filter to `null` are removed
 *
 * q: does this guarantee that any JSON produced by filtering by schema is also valid by the schema?
 *    --> think so at least for basics; this is what we should strive for. doubt is long tail of
 *        JSON schema features
 *          - for example a pattern on a required property.  If property value is invalid per
 *            pattern, then it will be removed, but it's still required, so result after filter is
 *            still invalid
 *
 *
 * reference implementations of similar JSON schema filtering logic, in case we want to compare our
 * semantics to theirs:
 *   - https://github.com/alank64/json-schema-filter
 *
 */
public class Filter {


    public Object filterBySchema(JsonNode provisionalOutput, JsonSchema schema, RefEnvironment environment) {
        if (schema.isRef()) {
            return filterBySchema(provisionalOutput, environment.resolve(schema.getRef()), environment);
        } else if (schema.hasType()) {
            //must have explicit type

            // https://json-schema.org/understanding-json-schema/reference/type.html
            if (schema.isString()) {
                if (provisionalOutput.isTextual()) {
                    //TODO: validate 'format'??
                    return provisionalOutput.asText();
                } else {
                    return null;
                }
            } else if (schema.isNumber()) {
                if (provisionalOutput.isNumber() || provisionalOutput.isNull()) {
                    return provisionalOutput.numberValue();
                } else {
                    return null;
                }
            } else if (schema.isInteger()) {
                if (provisionalOutput.canConvertToInt() || provisionalOutput.isNull()) {
                    return provisionalOutput.intValue();
                } else if (provisionalOutput.canConvertToLong()) {
                    return provisionalOutput.longValue();
                } else {
                    return null;
                }
            } else if (schema.isBoolean()) {
                if (provisionalOutput.isBoolean() || provisionalOutput.isNull()) {
                    return provisionalOutput.booleanValue();
                } else {
                    return null;
                }
            } else if (schema.isObject()) {
                if (provisionalOutput.isObject()) {
                    Map<String, Object> filtered = new HashMap<>();
                    provisionalOutput.fields().forEachRemaining(entry -> {
                        String key = entry.getKey();
                        JsonNode value = entry.getValue();
                        if (schema.getProperties() == null || !schema.getProperties().containsKey(key)) {
                            if (schema.getAdditionalPropertiesOrDefault()) {
                                filtered.put(key, value);
                            }
                        } else {
                            JsonSchema propertySchema = schema.getProperties().get(key);
                            Object filteredValue = filterBySchema(value, propertySchema, environment);
                            filtered.put(key, filteredValue);
                        }
                    });

                    //TODO: add support for `additionalProperties == true`? not expected use-case for
                    // proxy ...

                    // handler for additionalProperties??


                    return filtered;
                } else {
                    return null;
                }
            } else if (schema.isArray()) {
                if (provisionalOutput.isArray()) {
                    List<Object> filtered = new LinkedList<>();
                    provisionalOutput.elements().forEachRemaining(item -> {
                        // no items schema specified means any element is valid

                        Object filteredItem =
                            schema.getItems() == null ? item : filterBySchema(item, schema.getItems(), environment);

                        //q: this removes invalid items from the array; would we rather replace with `null`?
                        if (filteredItem != null) {
                            filtered.add(filteredItem);
                        }
                    });
                    return filtered;
                } else {
                    return null;
                }
            } else if (schema.isNull()) {
                //this is kinda nonsensical, right??
                // omit the property --> don't get it
                // include property with {type: null} --> get it, but it's always null?
                // or do we want to FAIL if value from source is NON-NULL?
                return null;
            } else {
                throw new IllegalArgumentException("Unknown schema type: " + schema);
            }
        } else {
            throw new IllegalArgumentException("Only schema with 'type' or '$ref' are supported: " + schema);
        }
    }
}
