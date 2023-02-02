package com.avaulta.gateway.rules;

import com.avaulta.gateway.rules.api.EndpointSpec;
import com.avaulta.gateway.rules.api.MethodSpec;
import com.avaulta.gateway.rules.api.ParameterSpec;
import com.avaulta.gateway.rules.api.RESTRules;
import com.avaulta.gateway.rules.jsonschema.JsonSchema;
import com.avaulta.gateway.rules.jsonschema.Validator;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RESTRulesRequestValidatorTest {

    ObjectMapper objectMapper = new ObjectMapper();
    Validator schemaValidator = new Validator();

    RESTRulesRequestValidator requestValidator;

    EndpointSpec userEndpoint = EndpointSpec.builder()
        .get(MethodSpec.builder()
            .pathParameters(List.of(
                ParameterSpec.builder()
                    .name("userId")
                    .schema(JsonSchema.builder()
                        .type("string")
                        .build())
                    .build()))
            .build()
        )
        .build();

    EndpointSpec usersEndpoint = EndpointSpec.builder()
        .get(MethodSpec.builder().build())
        .build();

    RESTRules restRules;

    @BeforeEach
    void setUp() {
        restRules = RESTRules.builder()
            .paths(Map.of(
                "/users/{userId}", userEndpoint,
                "/users", usersEndpoint
            ))
            .build();

        requestValidator = new RESTRulesRequestValidator(objectMapper, schemaValidator, restRules);
    }

    @Test
    void matchMethod() {
        RequestValidator.HttpRequest request = mock(RequestValidator.HttpRequest.class);
        when(request.getPath()).thenReturn("/users");
        when(request.getMethod()).thenReturn("GET");

        Optional<MethodSpec> method = requestValidator.matchMethod(request);

        assertTrue(method.isPresent());
        assertEquals(usersEndpoint.getGet(), method.get());

        RequestValidator.HttpRequest requestUser = mock(RequestValidator.HttpRequest.class);
        when(requestUser.getPath()).thenReturn("/users/123");
        when(requestUser.getMethod()).thenReturn("GET");

        Optional<MethodSpec> userMethod = requestValidator.matchMethod(requestUser);
        assertTrue(userMethod.isPresent());
        assertEquals(userEndpoint.getGet(), userMethod.get());
    }

    @CsvSource({
        "/users,/users",
        "'/users/([^/]+)','/users/{userId}'",
        "'/users/([^/]+)/messages','/users/{userId}/messages'",
        "'/users/([^/]+)/messages/([^/]+)','/users/{userId}/messages/{messageId}'",
    })
    @ParameterizedTest
    void toPattern(String expectedPattern, String pathTemplate) {
        assertEquals(
            expectedPattern,
                requestValidator.toPattern(pathTemplate).getKey().pattern());

    }

    @SneakyThrows
    @CsvSource({
        //1 as number, string, or integer, but not boolean
        "true,  number,  1",
        "true,  string,  1",
        "true,  integer, 1",
        "false, boolean, 1",

        //one as string, but not number, integer, or boolean
        "true,  string,  one",
        "false, number,  one",
        "false, integer, one",
        "false, boolean, one",

        //true as boolean or string, but not number
        "false, number,  true",
        "true,  boolean, true",
        "true,  string,  true",
    })
    @ParameterizedTest
    void isUrlSequenceValid(Boolean isValid, String type, String rawValue) {
        JsonSchema schema = JsonSchema.builder()
            .type(type)
            .build();

        assertEquals(isValid, requestValidator.isUrlSequenceValid(rawValue, schema, schema));
    }

    @Test
    void parseParams() {

        List<String> result0 = requestValidator.parseParams("/users");
        assertEquals(0, result0.size());

        List<String> result1 = requestValidator.parseParams("/users/{userId}");
        assertEquals(1, result1.size());
        assertEquals("userId", result1.get(0));

        List<String> result2 = requestValidator.parseParams("/users/{userId}/messages/{messageId}");
        assertEquals(2, result2.size());
        assertEquals("userId", result2.get(0));
        assertEquals("messageId", result2.get(1));
    }
}
