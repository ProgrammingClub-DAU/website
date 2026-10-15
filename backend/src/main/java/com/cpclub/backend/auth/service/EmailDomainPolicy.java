package com.cpclub.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enforces university email domain restrictions.
 *
 * <p>Trims and lowercases the email address, then requires an exact domain match
 * against {@code cpclub.auth.allowed-email-domains}. A subdomain such as
 * {@code student.dau.ac.in} must be explicitly listed on its own.</p>
 */
@Component
public class EmailDomainPolicy {

    private final Set<String> allowedDomains;
    private final String allowedDomainsSummary;

    public EmailDomainPolicy(@Value("${cpclub.auth.allowed-email-domains:dau.ac.in}") String allowedDomainsConfig) {
        if (allowedDomainsConfig == null || allowedDomainsConfig.isBlank()) {
            this.allowedDomains = Collections.emptySet();
            this.allowedDomainsSummary = "";
        } else {
            this.allowedDomains = Arrays.stream(allowedDomainsConfig.split(","))
                    .map(String::trim)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
            this.allowedDomainsSummary = String.join(", ", this.allowedDomains);
        }
    }

    /**
     * Checks if the given email has an exact allowed domain.
     *
     * @param email input email (untrimmed or mixed-case)
     * @return true if allowed, false otherwise
     */
    public boolean isAllowed(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = normalized.lastIndexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return false;
        }

        String domain = normalized.substring(atIndex + 1);
        return allowedDomains.contains(domain);
    }

    public Set<String> getAllowedDomains() {
        return allowedDomains;
    }

    public String getAllowedDomainsSummary() {
        return allowedDomainsSummary;
    }
}
