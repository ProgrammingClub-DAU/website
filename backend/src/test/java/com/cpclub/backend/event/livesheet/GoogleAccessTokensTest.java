package com.cpclub.backend.event.livesheet;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleAccessTokensTest {

    private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");

    private TestServiceAccount account;
    private ServiceAccountKey key;
    private RestTemplate restTemplate;
    private MockRestServiceServer google;

    @BeforeEach
    void setUp() {
        account = new TestServiceAccount();
        key = ServiceAccountKey.parse(account.json(), JsonMapper.builder().build());
        restTemplate = new RestTemplate();
        google = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("Exchanges a signed assertion Google can verify for an access token")
    void fetchesAToken() {
        AtomicReference<String> assertion = new AtomicReference<>();
        google.expect(requestTo(TestServiceAccount.TOKEN_URI))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(request -> assertion.set(formField(request, "assertion")))
                .andRespond(withSuccess("{\"access_token\":\"tok-1\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));

        String token = new GoogleAccessTokens(key, restTemplate, Clock.fixed(NOW, ZoneOffset.UTC)).get();

        assertEquals("tok-1", token);
        google.verify();

        // Signed with the service account's key, for the Sheets scope, by that account.
        Claims claims = Jwts.parser()
                .verifyWith(account.keyPair.getPublic())
                .clock(() -> java.util.Date.from(NOW))
                .build()
                .parseSignedClaims(assertion.get())
                .getPayload();
        assertEquals(TestServiceAccount.EMAIL, claims.getIssuer());
        assertEquals(GoogleAccessTokens.SHEETS_SCOPE, claims.get("scope"));

        // Google requires aud to be exactly its URL as a string, not an array.
        String payload = new String(Base64.getUrlDecoder().decode(assertion.get().split("\\.")[1]),
                StandardCharsets.UTF_8);
        assertTrue(payload.contains("\"aud\":\"" + TestServiceAccount.TOKEN_URI + "\""), payload);
    }

    @Test
    @DisplayName("Reuses the token until shortly before it expires, then fetches a new one")
    void cachesTheToken() {
        google.expect(requestTo(TestServiceAccount.TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"tok-1\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));
        google.expect(requestTo(TestServiceAccount.TOKEN_URI))
                .andRespond(withSuccess("{\"access_token\":\"tok-2\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));

        MutableClock clock = new MutableClock(NOW);
        GoogleAccessTokens tokens = new GoogleAccessTokens(key, restTemplate, clock);

        assertEquals("tok-1", tokens.get());
        clock.now = NOW.plusSeconds(3000);            // still well inside the hour
        assertEquals("tok-1", tokens.get());
        clock.now = NOW.plusSeconds(3600 - 30);       // inside the renewal margin
        assertEquals("tok-2", tokens.get());

        google.verify();
    }

    private static String formField(org.springframework.http.client.ClientHttpRequest request, String name) {
        String body = ((MockClientHttpRequest) request).getBodyAsString();
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        throw new AssertionError("No " + name + " in " + body);
    }

    /** A clock the test can move forward. */
    private static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
