package com.cpclub.backend.auth;

import com.cpclub.backend.auth.service.EmailVerificationService;
import com.cpclub.backend.mail.entity.EmailVerificationToken;
import com.cpclub.backend.mail.repository.EmailVerificationTokenRepository;
import com.cpclub.backend.mail.service.MailService;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MailService mailService;

    private EmailVerificationService verificationService;

    private User user;

    @BeforeEach
    void setUp() {
        verificationService = new EmailVerificationService(
                tokenRepository, userRepository, mailService, "http://localhost:3000");

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@dau.ac.in")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("Issue a token - DB holds the SHA-256 hex, never the raw token")
    void issueToken_storesSha256HexNotRawToken() {
        String rawToken = verificationService.issue(user);

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(captor.capture());

        EmailVerificationToken savedToken = captor.getValue();
        assertNotEquals(rawToken, savedToken.getTokenHash());
        assertEquals(EmailVerificationService.sha256Hex(rawToken), savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
    }

    @Test
    @DisplayName("Expired, reused, or unknown token - same 400 message for all three")
    void verifyToken_sameErrorMessageForExpiredReusedOrUnknown() {
        String rawToken = "sample-token-value-12345";
        String hash = EmailVerificationService.sha256Hex(rawToken);

        // Case 1: Unknown token
        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.empty());
        ResponseStatusException exUnknown = assertThrows(ResponseStatusException.class,
                () -> verificationService.verify(rawToken));
        assertEquals(HttpStatus.BAD_REQUEST, exUnknown.getStatusCode());
        assertEquals(EmailVerificationService.INVALID_OR_EXPIRED_MESSAGE, exUnknown.getReason());

        // Case 2: Reused (already used) token
        EmailVerificationToken reusedToken = EmailVerificationToken.builder()
                .id(1L)
                .user(user)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24))
                .usedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1))
                .build();
        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(reusedToken));
        ResponseStatusException exReused = assertThrows(ResponseStatusException.class,
                () -> verificationService.verify(rawToken));
        assertEquals(HttpStatus.BAD_REQUEST, exReused.getStatusCode());
        assertEquals(EmailVerificationService.INVALID_OR_EXPIRED_MESSAGE, exReused.getReason());

        // Case 3: Expired token
        EmailVerificationToken expiredToken = EmailVerificationToken.builder()
                .id(2L)
                .user(user)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(1))
                .usedAt(null)
                .build();
        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expiredToken));
        ResponseStatusException exExpired = assertThrows(ResponseStatusException.class,
                () -> verificationService.verify(rawToken));
        assertEquals(HttpStatus.BAD_REQUEST, exExpired.getStatusCode());
        assertEquals(EmailVerificationService.INVALID_OR_EXPIRED_MESSAGE, exExpired.getReason());
    }

    @Test
    @DisplayName("Issue twice - previous unused tokens are marked used so the first token no longer verifies")
    void issueTwice_marksEarlierTokensUsed() {
        verificationService.issue(user);
        verify(tokenRepository, times(1)).markUnusedAsUsedForUser(eq(user), any(LocalDateTime.class));

        verificationService.issue(user);
        verify(tokenRepository, times(2)).markUnusedAsUsedForUser(eq(user), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Valid token verification sets emailVerifiedAt and consumes token")
    void verifyToken_success() {
        String rawToken = "valid-token-sample";
        String hash = EmailVerificationService.sha256Hex(rawToken);

        EmailVerificationToken validToken = EmailVerificationToken.builder()
                .id(3L)
                .user(user)
                .tokenHash(hash)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(12))
                .usedAt(null)
                .build();

        when(tokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(validToken));

        verificationService.verify(rawToken);

        assertNotNull(validToken.getUsedAt());
        assertTrue(user.isEmailVerified());
        verify(tokenRepository).save(validToken);
        verify(userRepository).save(user);
    }
}
