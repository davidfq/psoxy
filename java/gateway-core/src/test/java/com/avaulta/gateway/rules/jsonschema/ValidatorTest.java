package com.avaulta.gateway.rules.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValidatorTest {

    Validator validator = new Validator();
    ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({
        "true,  'string', '\"some_string\"',",
        "false, 'string', '12312'",
        "false, 'string', '{\"property\": \"some_string\"}'",
        "false, 'string',  'false'",
        "false, 'number', '\"some_string\"',",
        "true,  'number', '12312'",
        "true,  'number', '123.12'",
        "false, 'number', '{\"property\": \"some_string\"}'",
        "false, 'number',  'false'",
        "false, 'boolean', '\"some_string\"',",
        "false, 'boolean', '12312'",
        "false, 'boolean', '{\"property\": \"some_string\"}'",
        "true,  'boolean',  'false'",
        "true,  'boolean',  'true'",
        "false, 'object', '\"some_string\"',",
        "false, 'object', '12312'",
        "true,  'object', '{\"property\": \"some_string\"}'",
        "false, 'object',  'false'",
        "false, 'array', '\"some_string\"',",
        "false, 'array', '12312'",
        "true,  'array', '[\"some_string\"]'",
        "false, 'array',  'false'",
    })
    void isValidPerSchema_primitives(Boolean expected, String type, String json) {
        JsonSchema schema = JsonSchema.builder()
            .type(type)
            .build();

        assertEquals(expected,
            validator.isValidPerSchema(objectMapper.readTree(json), schema, schema));
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({
        "true,  'string', '[\"some_string\"]'",
        "false, 'string', '12312'",
        "false, 'string', '{\"property\": \"some_string\"}'",
        "false, 'string',  'false'",
        "true,  'string',  []",
        "false, 'number', '\"some_string\"',",
        "true,  'number', '[12312]'",
        "true,  'number', '[123.12]'",
        "false, 'number', '{\"property\": \"some_string\"}'",
        "false, 'number',  'false'",
        "false, 'boolean', '\"some_string\"',",
        "false, 'boolean', '12312'",
        "false, 'boolean', '{\"property\": \"some_string\"}'",
        "true,  'boolean',  '[false]'",
        "true,  'boolean',  '[true]'",
        "true,  'boolean',  '[true, false]'",
        "true,  'boolean',  '[]'",
    })
    void isValidPerSchema_arrays(Boolean expected, String itemType, String json) {
        JsonSchema schema = JsonSchema.builder()
            .type("array")
            .items(JsonSchema.builder()
                .type(itemType)
                .build())
            .build();

        assertEquals(expected,
            validator.isValidPerSchema(objectMapper.readTree(json), schema, schema));
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({
        "false, 'string', '[\"some_string\"]'",
        "false, 'string', '12312'",
        "true,  'string', '{\"property\": \"some_string\"}'",
        "false, 'number', '{\"property\": \"some_string\"}'",
        "true,  'number', '{\"property\": 123}'",
        "true,  'number', '{\"property\": 123.23}'",
        "false, 'string',  'false'",
        "false, 'string',  []",
        "false, 'number', '\"some_string\"',"
    })
    void isValidPerSchema_objects(Boolean expected, String propertyType, String json) {
        JsonSchema schema = JsonSchema.builder()
            .type("object")
            .properties(Map.of("property", JsonSchema.builder()
                .type(propertyType)
                .build()))
            .build();

        assertEquals(expected,
            validator.isValidPerSchema(objectMapper.readTree(json), schema, schema));
    }

    @SneakyThrows
    @ParameterizedTest
    @CsvSource({
        "true,  true, '{\"property\": \"some_string\"}'",
        "false, false, '{\"property\": \"some_string\"}'",
    })
    void isValidPerSchema_objectNoProperties(Boolean expected, Boolean additionalProperties, String json) {
        JsonSchema schema = JsonSchema.builder()
            .type("object")
            .additionalProperties(additionalProperties)
            .build();

        assertEquals(expected,
            validator.isValidPerSchema(objectMapper.readTree(json), schema, schema));
    }
}
