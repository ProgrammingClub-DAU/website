package com.cpclub.backend.auth.service;

import com.cpclub.backend.mail.entity.EmailVerificationToken;
import com.cpclub.backend.mail.repository.EmailVerificationTokenRepository;
import com.cpclub.backend.mail.service.MailService;
import com.cpclub.backend.mail.service.MailTemplates;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

/**
 * Handles issuing and verifying single-use email verification tokens.
 *
 * <p>Only the SHA-256 hex digest of a token is stored in the database; the raw
 * token only ever exists in the verification email sent to the member.
 * All validation failures return an identical generic error message to prevent
 * token probing.</p>
 */
@Service
@Slf4j
public class EmailVerificationService {

    public static final String INVALID_OR_EXPIRED_MESSAGE = "This link is invalid or has expired";

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;
    private final String publicSiteUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            MailService mailService,
            @Value("${cpclub.public-site-url:http://localhost:3000}") String publicSiteUrl) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.mailService = mailService;
        this.publicSiteUrl = publicSiteUrl;
    }

    /**
     * Issues a new verification token for the user, marks any previous unused tokens as used,
     * persists the SHA-256 hash, and dispatches the verification email.
     *
     * @param user target member
     * @return raw URL-safe token
     */
    @Transactional
    public String issue(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String hash = sha256Hex(rawToken);

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        tokenRepository.markUnusedAsUsedForUser(user, now);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(now.plusHours(24))
                .build();
        tokenRepository.save(token);

        String baseUrl = publicSiteUrl != null ? publicSiteUrl.replaceAll("/+$", "") : "http://localhost:3000";
        String verificationUrl = baseUrl + "/verify-email?token=" + rawToken;
        String text = MailTemplates.verificationText(user.getName(), verificationUrl);
        String html = MailTemplates.verificationHtml(user.getName(), verificationUrl);

        mailService.send(user.getEmail(), "Verify your CP Club email", text, html);
        return rawToken;
    }

    /**
     * Verifies the given raw token, marking it consumed and setting {@code email_verified_at}
     * on the associated user.
     *
     * @param rawToken raw token received from the query parameter or body
     * @throws ResponseStatusException with 400 Bad Request on any failure
     */
    @Transactional
    public void verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_MESSAGE);
        }

        String hash = sha256Hex(rawToken.trim());
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_MESSAGE));

        if (token.isUsed() || token.isExpired()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, INVALID_OR_EXPIRED_MESSAGE);
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        token.setUsedAt(now);
        tokenRepository.save(token);

        User user = token.getUser();
        user.setEmailVerifiedAt(now);
        userRepository.save(user);

        log.info("Email verified successfully for user: {}", user.getEmail());
    }

    public static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }
}
