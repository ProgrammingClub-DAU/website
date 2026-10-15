package com.cpclub.backend.auth.service;

import com.cpclub.backend.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guard that enforces email verification for member write operations.
 *
 * <p>When {@code cpclub.auth.require-email-verification} is enabled, ensures that
 * the acting member has verified their email address, throwing HTTP 403 Forbidden
 * with reason {@code EMAIL_NOT_VERIFIED} if unverified.</p>
 */
@Component
public class VerifiedMemberGuard {

    private final boolean requireEmailVerification;

    public VerifiedMemberGuard(
            @Value("${cpclub.auth.require-email-verification:false}") boolean requireEmailVerification) {
        this.requireEmailVerification = requireEmailVerification;
    }

    public boolean isRequireEmailVerification() {
        return requireEmailVerification;
    }

    public void require(User user) {
        if (!requireEmailVerification) {
            return;
        }

        if (user == null || !user.isEmailVerified()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED");
        }
    }
}
