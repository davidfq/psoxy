package com.avaulta.gateway.rules;

import com.avaulta.gateway.rules.api.JsonSchema;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaDraft;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaRuleUtilsTest {

    SchemaRuleUtils schemaRuleUtils;
    ObjectMapper objectMapper;
    ObjectMapper yamlMapper;

    @BeforeEach
    void setup() {
        schemaRuleUtils = new SchemaRuleUtils();

        objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); //want ISO-strings

        yamlMapper = new YAMLMapper()
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        schemaRuleUtils.objectMapper = objectMapper;
        schemaRuleUtils.jsonSchemaGenerator = new JsonSchemaGenerator(schemaRuleUtils.objectMapper,
            JsonSchemaConfig
                //.nullableJsonSchemaDraft4() // uses oneOf [ { type: null }, ... ] everywhere, which is verbose
                .vanillaJsonSchemaDraft4()

                .withJsonSchemaDraft(JsonSchemaDraft.DRAFT_2019_09)
        );
    }

    @SneakyThrows
    @Test
    void generateJsonSchema() {
        JsonSchema jsonSchema = schemaRuleUtils.generateJsonSchema(SimplePojo.class);

        String jsonSchemaAsString = objectMapper
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(jsonSchema);

        //we want these serialized as ISO strings, so shouldn't have definitions
        assertFalse(jsonSchemaAsString.contains("#/definitions/Instant"));
        assertFalse(jsonSchemaAsString.contains("#/definitions/LocalDate"));

        assertEquals(SimplePojo.EXPECTED_SCHEMA, jsonSchemaAsString);

        assertEquals(SimplePojo.EXPECTED_SCHEMA_YAML,
            yamlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonSchema));
    }

    @SneakyThrows
    @Test
    void filterBySchema() {

        SimplePojoPlus simplePlus = SimplePojoPlus.builder()
            .someString("some-string")
            .date(LocalDate.parse("2023-01-16"))
            .timestamp(Instant.parse("2023-01-16T05:12:34Z"))
            .someListItem("list-item-1")
            .build();

        Object filteredToSimplePlus = schemaRuleUtils.filterObjectBySchema(simplePlus,
            schemaRuleUtils.generateJsonSchema(SimplePojoPlus.class));


        assertEquals("{\n" +
                "  \"date\" : \"2023-01-16\",\n" +
                "  \"someString\" : \"some-string\",\n" +
                "  \"someListItems\" : [ \"list-item-1\" ],\n" +
                "  \"timestamp\" : \"2023-01-16T05:12:34Z\"\n" +
                "}",
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredToSimplePlus));


        Object filteredToSimple = schemaRuleUtils.filterObjectBySchema(simplePlus,
            schemaRuleUtils.generateJsonSchema(SimplePojo.class));

        assertEquals("{\n" +
                "  \"date\" : \"2023-01-16\",\n" +
                "  \"someString\" : \"some-string\",\n" +
                "  \"timestamp\" : \"2023-01-16T05:12:34Z\"\n" +
                "}",
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredToSimple));

    }

    @Builder
    @Data
    static class SimplePojoPlus {

        String someString;

        LocalDate date;

        Instant timestamp;

        @Singular
        List<String> someListItems;
    }


    @Data
    static class SimplePojo {

        String someString;

        LocalDate date;

        Instant timestamp;

        static final String EXPECTED_SCHEMA = "{\n" +
            "  \"$schema\" : \"http://json-schema.org/draft/2019-09/schema#\",\n" +
            "  \"type\" : \"object\",\n" +
            "  \"additionalProperties\" : false,\n" +
            "  \"properties\" : {\n" +
            "    \"someString\" : {\n" +
            "      \"type\" : \"string\"\n" +
            "    },\n" +
            "    \"date\" : {\n" +
            "      \"type\" : \"string\",\n" +
            "      \"format\" : \"date\"\n" +
            "    },\n" +
            "    \"timestamp\" : {\n" +
            "      \"type\" : \"string\",\n" +
            "      \"format\" : \"date-time\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        static final String EXPECTED_SCHEMA_YAML = "---\n" +
            "$schema: \"http://json-schema.org/draft/2019-09/schema#\"\n" +
            "type: \"object\"\n" +
            "additionalProperties: false\n" +
            "properties:\n" +
            "  someString:\n" +
            "    type: \"string\"\n" +
            "  date:\n" +
            "    type: \"string\"\n" +
            "    format: \"date\"\n" +
            "  timestamp:\n" +
            "    type: \"string\"\n" +
            "    format: \"date-time\"\n";

    }

    @SneakyThrows
    @Test
    void filterBySchema_refs() {

        JsonSchema schemaWithRefs =
            schemaRuleUtils.generateJsonSchema(ComplexPojo.class);

        SimplePojoPlus simplePlus = SimplePojoPlus.builder()
            .someString("some-string")
            .date(LocalDate.parse("2023-01-16"))
            .timestamp(Instant.parse("2023-01-16T05:12:34Z"))
            .someListItem("list-item-1")
            .build();


        assertEquals(ComplexPojo.EXPECTED_SCHEMA,
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaWithRefs));

        assertEquals("{\n" +
                "  \"additionalSimplePojos\" : [ {\n" +
                "    \"date\" : \"2023-01-16\",\n" +
                "    \"someString\" : \"some-string\",\n" +
                "    \"timestamp\" : \"2023-01-16T05:12:34Z\"\n" +
                "  } ],\n" +
                "  \"simplePojo\" : {\n" +
                "    \"date\" : \"2023-01-16\",\n" +
                "    \"someString\" : \"some-string\",\n" +
                "    \"timestamp\" : \"2023-01-16T05:12:34Z\"\n" +
                "  }\n" +
                "}",
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(schemaRuleUtils.filterObjectBySchema(ComplexPojoPlus.builder()
                .simplePojo(simplePlus)
                .additionalSimplePojo(simplePlus)
                .build(), schemaWithRefs)));

    }

    @Builder
    @Data
    static class ComplexPojo {

        public static final String EXPECTED_SCHEMA = "{\n" +
            "  \"$schema\" : \"http://json-schema.org/draft/2019-09/schema#\",\n" +
            "  \"type\" : \"object\",\n" +
            "  \"additionalProperties\" : false,\n" +
            "  \"properties\" : {\n" +
            "    \"simplePojo\" : {\n" +
            "      \"$ref\" : \"#/definitions/SimplePojo\"\n" +
            "    },\n" +
            "    \"additionalSimplePojos\" : {\n" +
            "      \"type\" : \"array\",\n" +
            "      \"items\" : {\n" +
            "        \"$ref\" : \"#/definitions/SimplePojo\"\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"definitions\" : {\n" +
            "    \"SimplePojo\" : {\n" +
            "      \"type\" : \"object\",\n" +
            "      \"additionalProperties\" : false,\n" +
            "      \"properties\" : {\n" +
            "        \"someString\" : {\n" +
            "          \"type\" : \"string\"\n" +
            "        },\n" +
            "        \"date\" : {\n" +
            "          \"type\" : \"string\",\n" +
            "          \"format\" : \"date\"\n" +
            "        },\n" +
            "        \"timestamp\" : {\n" +
            "          \"type\" : \"string\",\n" +
            "          \"format\" : \"date-time\"\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";


        SimplePojo simplePojo;

        List<SimplePojo> additionalSimplePojos;
    }

    @Builder
    @Data
    static class ComplexPojoPlus {

        SimplePojoPlus simplePojo;

        @Singular
        List<SimplePojoPlus> additionalSimplePojos;
    }

}