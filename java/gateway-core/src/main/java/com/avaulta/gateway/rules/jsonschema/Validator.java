package com.avaulta.gateway.rules.jsonschema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Validate a JSON object against a JSON schema.
 */
@NoArgsConstructor
public class Validator {

    public boolean isValid(JsonNode node, JsonSchema schema, RefEnvironment refEnvironment) {
        if (schema.isRef()) {
            //q: do we need to try to resolve against schema too??
            // eg, can have `#/definitions/` not at root of schema?
            return isValid(node, refEnvironment.resolve(schema.getRef()), refEnvironment);
        } else if (schema.hasType()) {
            //must have explicit type

            // https://json-schema.org/understanding-json-schema/reference/type.html
            if (schema.isString()) {
                //TODO: validate 'format'??
                return node.isTextual();
            } else if (schema.isNumber()) {
                return node.isNumber() || node.isNull();
            } else if (schema.isInteger()) {
                return node.canConvertToInt() || node.isNull() || node.canConvertToLong();
            } else if (schema.isBoolean()) {
                return node.isBoolean() || node.isNull();
            } else if (schema.isObject()) {
                if (node.isObject()) {
                    boolean valid = true;
                    for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
                        Map.Entry<String, JsonNode> field = it.next();
                        String key = field.getKey();
                        JsonNode value = field.getValue();

                        if (schema.getProperties() == null) {
                            // no properties defined, so any property is valid if additionalProperties==true
                            valid = schema.getAdditionalProperties();
                        } else {
                            JsonSchema propertySchema = schema.getProperties().get(key);
                            if (propertySchema == null) {
                                //property not explicitly defined in schema, so valid if additionalProperties==true
                                valid = schema.getAdditionalPropertiesOrDefault();
                            } else {
                                valid = isValid(value, propertySchema, refEnvironment);
                            }
                        }
                    }

                    //q: what about required properties??
                    // - not relevant atm for how this is used in proxy, which is just to validate
                    // request parameters (for which object case really isn't expected anyways)

                    return valid;
                } else {
                    return node.isNull();
                }
            } else if (schema.isArray()) {
                if (node.isArray()) {
                    boolean valid = true;

                    //q: this correct? if no item type specified, allow anything??
                    if (schema.getItems() != null) {
                        for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                            JsonNode element = it.next();
                            valid = isValid(element, schema.getItems(), refEnvironment);
                        }
                    }

                    return valid;
                } else {
                    return node.isNull();
                }
            } else if (schema.isNull()) {
                return node.isNull();
            } else {
                throw new IllegalArgumentException("Unknown schema type: " + schema);
            }
        } else {
            throw new IllegalArgumentException("Only schema with 'type' or '$ref' are supported: " + schema);
        }
    }


}
