package com.cpclub.backend.mail.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Signed one-click unsubscribe links (D29).
 *
 * <p>A token is {@code base64url("userId:kind:" + hex(HMAC-SHA256("userId:kind")))}.
 * It names the member and the one email type to turn off, and the signature is what
 * stops anyone from editing the id to unsubscribe someone else.</p>
 *
 * <p><b>No expiry, on purpose.</b> An unsubscribe link in an email from six months ago
 * must still work; that is what makes the email legitimate to send. The cost is that
 * a token is good forever -- acceptable, because all it can ever do is switch one
 * email type off for one member, and the member can switch it back on.</p>
 *
 * <p>The application refuses to start with mail enabled and no secret, in the same
 * way JwtUtils guards the JWT secret: sending reminders that cannot be unsubscribed
 * from is not a degraded mode, it is a mode that must not exist.</p>
 */
@Service
public class UnsubscribeTokenService {

    /** Which preference a token turns off. */
    public enum Kind {
        EVENTS,
        CONTESTS
    }

    /** What a valid token names. */
    public record Claim(Long userId, Kind kind) {
    }

    /** 32 characters, the shortest secret this accepts with mail enabled. */
    static final int MIN_SECRET_LENGTH = 32;

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] key;

    public UnsubscribeTokenService(
            @Value("${cpclub.mail.enabled:false}") boolean mailEnabled,
            @Value("${cpclub.mail.unsubscribe-secret:}") String secret) {
        boolean missing = secret == null || secret.isBlank();
        if (mailEnabled && (missing || secret.trim().length() < MIN_SECRET_LENGTH)) {
            throw new IllegalStateException("CPCLUB_UNSUBSCRIBE_SECRET must be set to at least "
                    + MIN_SECRET_LENGTH + " characters when mail is enabled: every reminder carries a signed"
                    + " unsubscribe link. Generate one with: openssl rand -hex 32");
        }
        this.key = missing ? null : secret.trim().getBytes(StandardCharsets.UTF_8);
    }

    public String issue(Long userId, Kind kind) {
        if (key == null) {
            throw new IllegalStateException("No unsubscribe secret configured");
        }
        String payload = userId + ":" + kind.name();
        String token = payload + ":" + HexFormat.of().formatHex(hmac(payload));
        return ENCODER.encodeToString(token.getBytes(StandardCharsets.UTF_8));
    }

    /** Empty for anything malformed, unsigned or tampered with -- never an exception. */
    public Optional<Claim> verify(String token) {
        if (key == null || token == null || token.isBlank() || token.length() > 512) {
            return Optional.empty();
        }
        String decoded;
        try {
            decoded = new String(DECODER.decode(token.trim()), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        int last = decoded.lastIndexOf(':');
        if (last < 0) {
            return Optional.empty();
        }
        String payload = decoded.substring(0, last);
        byte[] given;
        try {
            given = HexFormat.of().parseHex(decoded.substring(last + 1));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
        // Constant time, so the comparison leaks nothing about how close a guess was.
        if (!MessageDigest.isEqual(hmac(payload), given)) {
            return Optional.empty();
        }
        String[] parts = payload.split(":");
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Claim(Long.parseLong(parts[0]), Kind.valueOf(parts[1])));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }
}
