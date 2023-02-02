package co.worklytics.psoxy.rules;

import co.worklytics.psoxy.PsoxyModule;
import co.worklytics.psoxy.gateway.impl.CommonRequestHandler;
import co.worklytics.test.MockModules;
import com.avaulta.gateway.rules.Endpoint;
import dagger.Component;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Rules2RequestValidatorTest {


    @Inject
    Rules2RequestValidatorFactory factory;

    Rules2RequestValidator requestValidator;



    @Singleton
    @Component(modules = {
        PsoxyModule.class,
        MockModules.ForConfigService.class,
    })
    public interface Container {
        void inject(Rules2RequestValidatorTest test);
    }
    @BeforeEach
    public void setup() {
        Rules2RequestValidatorTest.Container container = DaggerRules2RequestValidatorTest_Container.create();
        container.inject(this);

        requestValidator = factory.create((Rules2) PrebuiltSanitizerRules.DEFAULTS.get("gmail"));
    }

    @SneakyThrows
    @ValueSource(strings = {
        "https://gmail.googleapis.com/gmail/v1/users/me/messages/17c3b1911726ef3f?format=metadata",
        "https://gmail.googleapis.com/gmail/v1/users/me/messages",
    })
    @ParameterizedTest
    void allowedEndpointRegex_allowed(String url) {
        assertTrue(requestValidator.isAllowed(CommonRequestHandler.prototypeRequest("GET", new URL(url))));
    }

    @SneakyThrows
    @ValueSource(strings = {
        "https://gmail.googleapis.com/gmail/v1/users/me/threads",
        "https://gmail.googleapis.com/gmail/v1/users/me/profile",
        "https://gmail.googleapis.com/gmail/v1/users/me/settings/forwardingAddresses",
        "https://gmail.googleapis.com/gmail/v1/users/me/somethingPrivate/17c3b1911726ef3f\\?attemptToTrickRegex=messages",
        "https://gmail.googleapis.com/gmail/v1/users/me/should-not-pass?anotherAttempt=https://gmail.googleapis.com/gmail/v1/users/me/messages"
    })
    @ParameterizedTest
    void allowedEndpointRegex_blocked(String url) {
        assertFalse(requestValidator.isAllowed(CommonRequestHandler.prototypeRequest("GET", new URL(url))));
    }


    @SneakyThrows
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @ParameterizedTest
    void httpMethods_onlyGetAllowed(String notGet) {
        final URL EXAMPLE_URL = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages");
        Rules2RequestValidator strict = factory.create(Rules2.builder()
            .endpoint(Endpoint.builder()
                .allowedMethods(Collections.singleton("GET"))
                .pathRegex("^/gmail/v1/users/[^/]*/messages[/]?.*?$")
                .build())
            .build());

        assertTrue(strict.isAllowed(CommonRequestHandler.prototypeRequest("GET", EXAMPLE_URL)));
        assertFalse(strict.isAllowed(CommonRequestHandler.prototypeRequest(notGet, EXAMPLE_URL)));
    }


    @SneakyThrows
    @ValueSource(strings = { "GET", "POST", "PUT", "PATCH" })
    @ParameterizedTest
    void allHttpMethodsAllowed(String httpMethod) {
        final URL EXAMPLE_URL = new URL("https://gmail.googleapis.com/gmail/v1/users/me/messages");
        assertTrue(requestValidator.isAllowed(CommonRequestHandler.prototypeRequest(httpMethod, EXAMPLE_URL)));
    }
}
