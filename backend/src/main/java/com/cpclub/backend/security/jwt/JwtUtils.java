package com.cpclub.backend.security.jwt;

import com.cpclub.backend.security.service.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Set;

/**
 * Creates and verifies signed JSON Web Tokens used by the stateless security layer.
 *
 * <p>Tokens carry only the principal email, identifier, and authority. Signature and expiry
 * validation happen before a request is considered authenticated.</p>
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private int jwtExpirationMs;

    /**
     * Secrets that must never sign a real token.
     *
     * <p>The first entry was committed to this repository as a fallback default
     * for a period, so it is public in the git history and, being the canonical
     * value from a widely-copied tutorial, appears in thousands of other public
     * repositories. Anyone holding it can forge a token for any account,
     * including an administrator.
     *
     * <p>A length check alone cannot catch this: the value is 64 characters and
     * passes every strength test. It has to be rejected by identity.
     */
    private static final Set<String> KNOWN_COMPROMISED_SECRETS = Set.of(
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    );

    /**
     * Validates the JWT secret on application startup.
     *
     * <p>Deliberately fails startup rather than warning. A running application on
     * a compromised or weak secret looks completely healthy from the outside,
     * so there is nothing to notice until an account is taken over.
     */
    @jakarta.annotation.PostConstruct
    private void validateSecret() {
        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "[SECURITY] app.jwt.secret must be configured and at least 32 characters long. " +
                "Set the JWT_SECRET environment variable before starting the application."
            );
        }

        if (KNOWN_COMPROMISED_SECRETS.contains(jwtSecret.trim())) {
            throw new IllegalStateException(
                "[SECURITY] app.jwt.secret is a publicly known value that was previously " +
                "committed to this repository. Anyone can forge tokens with it. " +
                "Generate a new one (openssl rand -base64 48) and set JWT_SECRET before starting."
            );
        }
    }

    /**
     * Derives the HS256 signing key from the configured secret.
     *
     * @return HMAC key used to sign and verify tokens
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                jwtSecret.length() < 32 ?
                        java.util.Base64.getEncoder().encodeToString(String.format("%-32s", jwtSecret).getBytes()) :
                        java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        ));
    }

    /**
     * Issues a token from a successfully authenticated Spring Security principal.
     *
     * @param authentication established authentication containing {@link UserDetailsImpl}
     * @return compact signed JWT
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject((userPrincipal.getUsername()))
                .claim("id", userPrincipal.getId())
                .claim("role", userPrincipal.getAuthorities().stream()
                        .findFirst()
                        .map(a -> a.getAuthority())
                        .orElse("ROLE_USER"))
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Issues a token directly after registration, before a login authentication exists.
     *
     * @param email user email stored as the token subject
     * @param id persisted user identifier
     * @param role authorization role claim
     * @return compact signed JWT
     */
    public String generateTokenFromEmail(String email, Long id, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("id", id)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the authenticated email subject from a verified JWT.
     *
     * @param token compact JWT string
     * @return email subject embedded in the token
     * @throws JwtException if the token cannot be verified or parsed
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Checks the token signature, structure, and expiry without exposing parsing errors to callers.
     *
     * @param authToken compact JWT supplied in an authorization header
     * @return {@code true} only when the token is currently valid
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (JwtException e) {
            // Signature and other parser failures are invalid tokens, not server failures.
            log.error("JWT token verification failed: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
