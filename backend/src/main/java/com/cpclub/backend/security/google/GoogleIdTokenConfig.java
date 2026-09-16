package com.cpclub.backend.security.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;
import java.util.Set;

/**
 * Decoder for the ID tokens Google issues to the sign-in button.
 *
 * <p>Two different kinds of JWT pass through this application and they must not be
 * confused. The tokens this decoder reads are minted by Google, signed with
 * Google's keys, and are only ever accepted at the sign-in endpoint. The tokens
 * the rest of the app authenticates with are minted here, signed with our own
 * secret, and handled by {@code JwtUtils}. A Google token is a claim about who
 * someone is; it is exchanged once, for a token of ours, and then discarded.</p>
 *
 * <p>The bean is deliberately not wired into any filter chain. Spring Security
 * would happily treat a {@link JwtDecoder} as the way to authenticate every
 * request if the resource-server starter were on the classpath, which would let a
 * raw Google token act as a session. Only {@code spring-security-oauth2-jose} is
 * on the classpath, so no autoconfiguration picks this up, and the one consumer
 * asks for it by name.</p>
 */
@Configuration
// Eager, against the global spring.main.lazy-initialization=true used in
// production. Left lazy, a missing client ID would surface as a failed sign-in
// hours after a deploy rather than as a failed deploy -- and with sign-in being
// the only way in, that is the difference between a rollback and a lockout.
@Lazy(false)
public class GoogleIdTokenConfig {

    /** Where Google publishes the public keys its ID tokens are signed with. */
    private static final String GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /**
     * Both spellings Google uses for the issuer claim.
     *
     * <p>Google has issued tokens under each of these for years and documents both
     * as valid, so checking for only one would reject real sign-ins.</p>
     */
    private static final Set<String> GOOGLE_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    private final String clientId;

    public GoogleIdTokenConfig(@Value("${app.google.client-id}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("""
                    GOOGLE_CLIENT_ID is not set, so nobody could sign in.
                    Set it to the OAuth 2.0 Web client ID from Google Cloud Console -- the \
                    same value the frontend is built with as NEXT_PUBLIC_GOOGLE_CLIENT_ID. \
                    See documents/google_sign_in_setup.md.""");
        }
        this.clientId = clientId.trim();
    }

    /**
     * Builds the decoder that checks a Google ID token's signature and claims.
     *
     * <p>Key material is fetched from Google on first use and cached, so no network
     * call happens at startup and a Google outage cannot stop the app booting.</p>
     *
     * @return a decoder that accepts only unexpired Google tokens issued to this client
     */
    @Bean
    public JwtDecoder googleIdTokenDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build();

        // Default validators cover expiry and not-before. The two added here are
        // what make the token ours: issued by Google, and issued *for this app*.
        decoder.setJwtValidator(new DelegatingValidator(List.of(
                JwtValidators.createDefault(),
                issuedByGoogle(),
                issuedForThisClient()
        )));

        return decoder;
    }

    private OAuth2TokenValidator<Jwt> issuedByGoogle() {
        return token -> GOOGLE_ISSUERS.contains(String.valueOf(token.getIssuer()))
                ? OAuth2TokenValidatorResult.success()
                : failure("The token was not issued by Google.");
    }

    /**
     * Rejects a token minted for some other application.
     *
     * <p>Without this check, an ID token obtained by any site the member has signed
     * into with Google could be replayed here and accepted as proof of identity.
     * The audience claim is what ties a token to this client ID.</p>
     */
    private OAuth2TokenValidator<Jwt> issuedForThisClient() {
        return token -> token.getAudience() != null && token.getAudience().contains(clientId)
                ? OAuth2TokenValidatorResult.success()
                : failure("The token was issued for a different application.");
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }

    /**
     * Runs several validators and reports every failure, not just the first.
     *
     * <p>Spring ships {@code DelegatingOAuth2TokenValidator} for this, but it is
     * spelled out here so the sign-in path has no surprises: a token must satisfy
     * all of these, and a log line naming one failed check is easier to act on
     * than a generic rejection.</p>
     */
    private record DelegatingValidator(List<OAuth2TokenValidator<Jwt>> validators)
            implements OAuth2TokenValidator<Jwt> {

        @Override
        public OAuth2TokenValidatorResult validate(Jwt token) {
            for (OAuth2TokenValidator<Jwt> validator : validators) {
                OAuth2TokenValidatorResult result = validator.validate(token);
                if (result.hasErrors()) {
                    return result;
                }
            }
            return OAuth2TokenValidatorResult.success();
        }
    }
}
