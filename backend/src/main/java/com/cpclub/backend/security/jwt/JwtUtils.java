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

@Component
@Slf4j
public class JwtUtils {

    @Value("${cpclub.app.jwtSecret:cpClubDefaultJwtSecretKeyForSecurityTestingAndProductionUsageKey32BytesLong}")
    private String jwtSecret;

    @Value("${cpclub.app.jwtExpirationMs:86400000}")
    private int jwtExpirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                jwtSecret.length() < 32 ?
                        java.util.Base64.getEncoder().encodeToString(String.format("%-32s", jwtSecret).getBytes()) :
                        java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        ));
    }

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

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

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
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }
}
