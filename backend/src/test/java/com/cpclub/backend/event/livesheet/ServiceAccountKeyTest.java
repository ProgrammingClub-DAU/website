package com.cpclub.backend.event.livesheet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceAccountKeyTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final TestServiceAccount account = new TestServiceAccount();

    @Test
    @DisplayName("Reads the key file exactly as Google downloads it")
    void parsesTheJsonFile() {
        ServiceAccountKey key = ServiceAccountKey.parse(account.json(), objectMapper);

        assertEquals(TestServiceAccount.EMAIL, key.clientEmail());
        assertEquals(TestServiceAccount.TOKEN_URI, key.tokenUri());
        assertArrayEquals(account.keyPair.getPrivate().getEncoded(), key.privateKey().getEncoded());
    }

    @Test
    @DisplayName("Also accepts the file Base64-encoded, for dashboards that mangle line breaks")
    void parsesBase64() {
        String encoded = Base64.getEncoder().encodeToString(account.json().getBytes(StandardCharsets.UTF_8));

        ServiceAccountKey key = ServiceAccountKey.parse(encoded, objectMapper);

        assertEquals(TestServiceAccount.EMAIL, key.clientEmail());
    }

    @Test
    @DisplayName("Falls back to Google's token endpoint when the file does not name one")
    void defaultsTheTokenUri() {
        String withoutTokenUri = account.json().replace(",\"token_uri\":\"" + TestServiceAccount.TOKEN_URI + "\"", "");

        ServiceAccountKey key = ServiceAccountKey.parse(withoutTokenUri, objectMapper);

        assertEquals(ServiceAccountKey.DEFAULT_TOKEN_URI, key.tokenUri());
    }

    @Test
    @DisplayName("A key with no private_key is refused with a message naming the field")
    void rejectsAMissingField() {
        String noKey = "{\"client_email\":\"" + TestServiceAccount.EMAIL + "\"}";

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServiceAccountKey.parse(noKey, objectMapper));

        assertTrue(error.getMessage().contains("private_key"));
    }

    @Test
    @DisplayName("Something that is neither JSON nor Base64 is refused, not half-parsed")
    void rejectsGarbage() {
        assertThrows(IllegalArgumentException.class,
                () -> ServiceAccountKey.parse("not a key at all!", objectMapper));
    }

    @Test
    @DisplayName("toString never includes the private key")
    void toStringHidesThePrivateKey() {
        ServiceAccountKey key = ServiceAccountKey.parse(account.json(), objectMapper);

        assertFalse(key.toString().contains("PRIVATE"));
        assertTrue(key.toString().contains(TestServiceAccount.EMAIL));
    }
}
