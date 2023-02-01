package com.avaulta.gateway.rules;

import com.avaulta.gateway.rules.jsonschema.Filter;
import com.avaulta.gateway.rules.jsonschema.JsonSchema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import lombok.*;

@NoArgsConstructor(access = AccessLevel.PACKAGE) //for tests
@AllArgsConstructor
public class SchemaRuleUtils {

    ObjectMapper objectMapper;
    JsonSchemaGenerator jsonSchemaGenerator;

    Filter jsonSchemaFilter;

    /**
     * Generates a JSON schema for the given class.
     * <p>q
     * use case: in client code bases, can generate rules for a given expected result class; perhaps
     * eventually as a build step, eg maven plugin, that writes rules out somewhere
     * <p>
     * eg,  schemaRuleUtils.generateSchema(ExampleResult.class)
     *
     * @param clazz
     * @return
     */
    public JsonSchema generateJsonSchema(Class<?> clazz) {
        JsonNode schema = jsonSchemaGenerator.generateJsonSchema(clazz);
        return objectMapper.convertValue(schema, JsonSchema.class);
    }

    /**
     * filter object by properties defined in schema, recursively filtering them by any schema
     * specified for them as well.
     *
     * NOTE: `null` values will be returned for property specified in schema IF value is null in
     * object OR value is of type that doesn't match schema.
     *  (eg, `null` is considered to fulfill any type-constraint)
     *
     *   TODO support format
     *
     * q: do we want to support filtering by full JsonSchema here?? complex, and not always
     * well-defined.
     *
     *
     * @param object to filter
     * @param schema to filter object's properties by
     * @return object, if matches schema; or sub
     */
    public Object filterObjectBySchema(Object object, JsonSchema schema) {
        JsonNode provisionalOutput = objectMapper.valueToTree(object);
        return jsonSchemaFilter.filterBySchema(provisionalOutput, schema, schema);
    }

    @SneakyThrows
    public String filterJsonBySchema(String jsonString, JsonSchema schema) {
        JsonNode provisionalOutput = objectMapper.readTree(jsonString);
        return objectMapper.writeValueAsString(jsonSchemaFilter.filterBySchema(provisionalOutput, schema, schema));
    }



}
