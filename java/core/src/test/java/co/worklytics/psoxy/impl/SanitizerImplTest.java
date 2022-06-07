package co.worklytics.psoxy.impl;

import co.worklytics.psoxy.*;
import co.worklytics.psoxy.rules.PrebuiltSanitizerRules;
import co.worklytics.psoxy.rules.Rules1;
import co.worklytics.psoxy.rules.Transform;
import co.worklytics.test.MockModules;
import co.worklytics.test.TestUtils;
import dagger.Component;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SanitizerImplTest {

    static final String ALICE_CANONICAL = "alice@worklytics.co";

    SanitizerImpl sanitizer;


    @Inject
    protected SanitizerFactory sanitizerFactory;


    @Singleton
    @Component(modules = {
        PsoxyModule.class,
        MockModules.ForConfigService.class,
    })
    public interface Container {
        void inject(SanitizerImplTest test);
    }

    @BeforeEach
    public void setup() {
        Container container = DaggerSanitizerImplTest_Container.create();
        container.inject(this);

        sanitizer = sanitizerFactory.create(Sanitizer.Options.builder()
            .rules((Rules1) PrebuiltSanitizerRules.DEFAULTS.get("gmail"))
            .pseudonymizationSalt("an irrelevant per org secret")
            .defaultScopeId("scope")
            .build());
    }

    @SneakyThrows
    @Test
    void sanitize_poc() {

        String jsonPart = "{\n" +
            "        \"name\": \"To\",\n" +
            "        \"value\": \"ops@worklytics.co\"\n" +
            "      }";

        String jsonString = new String(TestUtils.getData("api-response-examples/g-workspace/gmail/message.json"));

        //verify precondition that example actually contains something we need to pseudonymize
        assertTrue(jsonString.contains(jsonPart));
        assertTrue(jsonString.contains("alice@worklytics.co"));
        assertTrue(jsonString.contains("Subject"));

        String sanitized = sanitizer.sanitize(new URL("https", "gmail.googleapis.com", "/gmail/v1/users/me/messages/17c3b1911726ef3f\\?format=metadata"), jsonString);


        //email address should disappear
        assertFalse(sanitized.contains(jsonPart));
        assertFalse(sanitized.contains(jsonPart.replaceAll("\\s","")));
        assertFalse(sanitized.contains("alice@worklytics.co"));

        //redaction should remove 'Subject' header entirely; and NOT just replace it with `null`
        assertFalse(sanitized.contains("Subject"));
        assertFalse(sanitized.contains("null"));
    }



    @ValueSource(strings = {
        "alice@worklytics.co",
        "Alice Example <alice@worklytics.co>",
        "\"Alice Example\" <alice@worklytics.co>",
        "Alice.Example@worklytics.co"
    })
    @ParameterizedTest
    void emailDomains(String mailHeaderValue) {
        assertEquals("worklytics.co", sanitizer.pseudonymize(mailHeaderValue).getDomain());
    }

    @ValueSource(strings = {
        ALICE_CANONICAL,
        "Alice Example <alice@worklytics.co>",
        "\"Alice Different Last name\" <alice@worklytics.co>",
        "Alice@worklytics.co",
        "AlIcE@worklytics.co",
    })
    @ParameterizedTest
    void emailCanonicalEquivalents(String mailHeaderValue) {
        PseudonymizedIdentity canonicalExample = sanitizer.pseudonymize(ALICE_CANONICAL);

        assertEquals(canonicalExample.getHash(),
            sanitizer.pseudonymize(mailHeaderValue).getHash());
    }

    @ValueSource(strings = {
        "bob@worklytics.co",
        "Alice Example <alice2@worklytics.co>",
        "\"Alice Example\" <alice-a@worklytics.co>",
        "Alice@somewhere-else.co",
        "AlIcE.Other@worklytics.co",
    })
    @ParameterizedTest
    void emailCanonicalDistinct(String mailHeaderValue) {
        PseudonymizedIdentity  canonicalExample = sanitizer.pseudonymize(ALICE_CANONICAL);

        assertNotEquals(canonicalExample.getHash(),
            sanitizer.pseudonymize(mailHeaderValue).getHash());
    }

    @ValueSource(strings = {
        "alice@worklytics.co, bob@worklytics.co",
        "\"Alice Example\" <alice@worklytics.co>, \"Bob Example\" <bob@worklytics.co>",
        "Alice.Example@worklytics.co,Bob@worklytics.co",
        // TODO: per RFC 2822, the following SHOULD work ... but indeed lib we're using fails on it
        //"Alice.Example@worklytics.co, , Bob@worklytics.co"
    })
    @ParameterizedTest
    void pseudonymize_multivalueEmailHeaders(String headerValue) {
        List<PseudonymizedIdentity> pseudonyms = sanitizer.pseudonymizeEmailHeader(headerValue);
        assertEquals(2, pseudonyms.size());
        assertTrue(pseudonyms.stream().allMatch(p -> Objects.equals("worklytics.co", p.getDomain())));
    }


    @Test
    void hashMatchesLegacy() {
        //value taken from legacy app

        final String CANONICAL = "original";

        //value taken from legacy app
        final String identityHash = "xqUOU_DGuUAw4ErZIFL4pGx3bZDrFfLU6jQC4ClhrJI";

        assertEquals(identityHash,
            sanitizer.pseudonymize(CANONICAL).getHash());
    }

    @SneakyThrows
    @ValueSource(strings = {
        "https://gmail.googleapis.com/gmail/v1/users/me/messages/17c3b1911726ef3f?format=metadata",
        "https://gmail.googleapis.com/gmail/v1/users/me/messages",
    })
    @ParameterizedTest
    void allowedEndpointRegex_allowed(String url) {
        assertTrue(sanitizer.isAllowed(new URL(url)));
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
        assertFalse(sanitizer.isAllowed(new URL(url)));
    }

    @SneakyThrows
    @ValueSource(strings = {
        "pwd=1234asAf",
        " pwd=1234asAf  ",
        "https://asdf.google.com/asdf/?pwd=1234asAf",
        "https://asdf.google.com/asdf/?pwd=1234asAf&pwd=14324",
        "https://asdf.google.com/asdf/?asdf=2134&pwd=1234asAf&"
    })
    @ParameterizedTest
    void redactRegexMatches(String source) {
        Transform.RedactRegexMatches transform = Transform.RedactRegexMatches.builder().regex("pwd=[^&]*").build();

        assertTrue(source.contains("pwd=1234asAf"));
        String redacted = (String) sanitizer.getRedactRegexMatches(transform).map(source, sanitizer.jsonConfiguration);
        assertFalse(redacted.contains("pwd=1234asAf"));
    }

}
