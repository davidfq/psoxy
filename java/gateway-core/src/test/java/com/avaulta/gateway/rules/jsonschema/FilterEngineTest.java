package com.avaulta.gateway.rules.jsonschema;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class FilterEngineTest {

    FilterEngine filterEngine = new FilterEngine();

    ObjectMapper objectMapper = new ObjectMapper();

    @SneakyThrows
    @CsvSource({
        "true,  string, '\"some_string\"'",
        "false, number, '\"some_string\"'",
        "false, boolean, '\"some_string\"'",
        "false, string, '1'",
        "true,  number, '1'",
        "false, boolean, '1'",
        "false, string, 'false'",
        "false, number, 'false'",
        "true,  boolean, 'false'",
        // untyped arrays
        "true,  array, '[\"some_string\"]'",
        "true,  array, '[1,2,3]'",
    })
    @ParameterizedTest
    void filter_primitive(Boolean pass, String typeFilter, String originalJson) {

        JsonSchema filterSchema = JsonSchema.builder()
            .type(typeFilter)
            .build();

        Object filteredResult =
            filterEngine.filterBySchema(objectMapper.readTree(originalJson), filterSchema, filterSchema);

        if (pass) {
            assertEquals(originalJson, objectMapper.writeValueAsString(filteredResult));
        } else {
            assertNotEquals(originalJson, objectMapper.writeValueAsString(filteredResult));
        }
    }

    @SneakyThrows
    @Test
    void filter_objectEmpty() {

        JsonSchema filterSchema = JsonSchema.builder()
            .type("object")
            .build();

        Object filteredResult =
            filterEngine.filterBySchema(objectMapper.readTree("{\"a\":\"blah\"}"), filterSchema, filterSchema);

        assertEquals("{}", objectMapper.writeValueAsString(filteredResult));
    }

    @SneakyThrows
    @Test
    void filter_objectAdditional() {

        JsonSchema filterSchema = JsonSchema.builder()
            .type("object")
            .additionalProperties(true)
            .build();

        Object filteredResult =
            filterEngine.filterBySchema(objectMapper.readTree("{\"a\":\"blah\"}"), filterSchema, filterSchema);

        assertEquals("{\"a\":\"blah\"}", objectMapper.writeValueAsString(filteredResult));
    }
}
