package com.cpclub.backend.event.livesheet;

import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Access tokens for the service account, from Google's OAuth token endpoint.
 *
 * <p>The server-to-server flow: sign a short JWT with the service account's
 * private key, send it to Google, get back an access token valid for an hour.
 * Done by hand with the JWT library the application already uses for its own
 * sessions, rather than with Google's client libraries, which would add tens of
 * megabytes to a server that runs in 512 MB.</p>
 *
 * <p>The token is cached and reused until a minute before it expires, so a busy
 * attendance session costs one token request an hour, not one per row.</p>
 */
public class GoogleAccessTokens {

    /** Read and write spreadsheets the service account has been given access to. */
    static final String SHEETS_SCOPE = "https://www.googleapis.com/auth/spreadsheets";

    /** Google caps a signed assertion at one hour. */
    private static final Duration ASSERTION_LIFETIME = Duration.ofHours(1);

    /** Renew this long before expiry, so a token never lapses mid-request. */
    private static final Duration RENEW_MARGIN = Duration.ofMinutes(1);

    private final ServiceAccountKey key;
    private final RestTemplate restTemplate;
    private final Clock clock;

    private String cachedToken;
    private Instant cachedUntil = Instant.EPOCH;

    public GoogleAccessTokens(ServiceAccountKey key, RestTemplate restTemplate, Clock clock) {
        this.key = key;
        this.restTemplate = restTemplate;
        this.clock = clock;
    }

    /**
     * A valid access token, fetching a new one only when the cached one is near
     * expiry.
     *
     * <p>Synchronized: two threads needing a token at once should share one
     * request, not race to make two.</p>
     *
     * @return a bearer token for the Sheets API
     */
    public synchronized String get() {
        Instant now = clock.instant();
        if (cachedToken != null && now.isBefore(cachedUntil)) {
            return cachedToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        form.add("assertion", signedAssertion(now));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<?, ?> response = restTemplate.postForObject(
                key.tokenUri(), new HttpEntity<>(form, headers), Map.class);

        if (response == null || !(response.get("access_token") instanceof String token)) {
            throw new IllegalStateException("Google returned no access token.");
        }

        long expiresIn = response.get("expires_in") instanceof Number seconds
                ? seconds.longValue()
                : ASSERTION_LIFETIME.toSeconds();

        cachedToken = token;
        cachedUntil = now.plusSeconds(expiresIn).minus(RENEW_MARGIN);
        return token;
    }

    /**
     * The JWT Google exchanges for an access token.
     *
     * <p>{@code aud} is written as a single string, not a one-element array:
     * Google's token endpoint requires exactly its own URL there.</p>
     */
    String signedAssertion(Instant now) {
        return Jwts.builder()
                .issuer(key.clientEmail())
                .audience().single(key.tokenUri())
                .claim("scope", SHEETS_SCOPE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ASSERTION_LIFETIME)))
                .signWith(key.privateKey(), Jwts.SIG.RS256)
                .compact();
    }
}
