package com.cpclub.backend.auth;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class VerifiedMemberGuardTest {

    @Test
    @DisplayName("When require-email-verification is false, unverified user is permitted")
    void disabled_allowsUnverifiedUser() {
        VerifiedMemberGuard guard = new VerifiedMemberGuard(false);
        User user = User.builder().id(1L).email("user@dau.ac.in").role(Role.ROLE_USER).build();
        assertDoesNotThrow(() -> guard.require(user));
    }

    @Test
    @DisplayName("When require-email-verification is true, unverified user is rejected with 403 EMAIL_NOT_VERIFIED")
    void enabled_rejectsUnverifiedUser() {
        VerifiedMemberGuard guard = new VerifiedMemberGuard(true);
        User user = User.builder().id(1L).email("user@dau.ac.in").role(Role.ROLE_USER).build();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> guard.require(user));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("EMAIL_NOT_VERIFIED", ex.getReason());
    }

    @Test
    @DisplayName("When require-email-verification is true, verified user is permitted")
    void enabled_allowsVerifiedUser() {
        VerifiedMemberGuard guard = new VerifiedMemberGuard(true);
        User user = User.builder()
                .id(1L)
                .email("user@dau.ac.in")
                .role(Role.ROLE_USER)
                .emailVerifiedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build();
        assertDoesNotThrow(() -> guard.require(user));
    }
}
