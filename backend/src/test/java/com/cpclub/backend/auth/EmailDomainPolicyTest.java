package com.cpclub.backend.auth;

import com.cpclub.backend.auth.service.EmailDomainPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailDomainPolicyTest {

    @Test
    @DisplayName("Whitespaces and mixed casing are trimmed and accepted")
    void shouldAcceptUntrimmedAndMixedCase() {
        EmailDomainPolicy policy = new EmailDomainPolicy("dau.ac.in");
        assertTrue(policy.isAllowed(" Name@DAU.ac.in "));
    }

    @Test
    @DisplayName("Subdomain is rejected when only parent domain is configured")
    void shouldRejectSubdomainWhenOnlyParentConfigured() {
        EmailDomainPolicy policy = new EmailDomainPolicy("dau.ac.in");
        assertFalse(policy.isAllowed("name@student.dau.ac.in"));
    }

    @Test
    @DisplayName("Both domains are accepted when two domains are configured")
    void shouldAcceptBothConfiguredDomains() {
        EmailDomainPolicy policy = new EmailDomainPolicy("dau.ac.in, student.dau.ac.in");
        assertTrue(policy.isAllowed("alice@dau.ac.in"));
        assertTrue(policy.isAllowed("bob@student.dau.ac.in"));
        assertFalse(policy.isAllowed("eve@gmail.com"));
    }

    @Test
    @DisplayName("Malformed emails are rejected")
    void shouldRejectMalformedEmails() {
        EmailDomainPolicy policy = new EmailDomainPolicy("dau.ac.in");
        assertFalse(policy.isAllowed(null));
        assertFalse(policy.isAllowed(""));
        assertFalse(policy.isAllowed("   "));
        assertFalse(policy.isAllowed("invalid-email"));
        assertFalse(policy.isAllowed("@dau.ac.in"));
    }
}
