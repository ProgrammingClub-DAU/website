package com.cpclub.backend.mail;

import com.cpclub.backend.mail.service.UnsubscribeTokenService;
import com.cpclub.backend.mail.service.UnsubscribeTokenService.Claim;
import com.cpclub.backend.mail.service.UnsubscribeTokenService.Kind;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsubscribeTokenServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private final UnsubscribeTokenService service = new UnsubscribeTokenService(true, SECRET);

    @Test
    @DisplayName("A token names the member and the email type it turns off")
    void roundTrip() {
        Optional<Claim> claim = service.verify(service.issue(42L, Kind.CONTESTS));

        assertTrue(claim.isPresent());
        assertEquals(42L, claim.get().userId());
        assertEquals(Kind.CONTESTS, claim.get().kind());
    }

    @Test
    @DisplayName("A token with a tampered user id is rejected")
    void tamperedUserId_isRejected() {
        String token = service.issue(42L, Kind.EVENTS);
        String decoded = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(decoded.replaceFirst("^42:", "43:").getBytes(StandardCharsets.UTF_8));

        assertFalse(service.verify(forged).isPresent(), "someone else's preferences must not be reachable");
    }

    @Test
    @DisplayName("A token with a tampered kind is rejected")
    void tamperedKind_isRejected() {
        String decoded = new String(Base64.getUrlDecoder().decode(service.issue(42L, Kind.EVENTS)), StandardCharsets.UTF_8);
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(decoded.replace(":EVENTS:", ":CONTESTS:").getBytes(StandardCharsets.UTF_8));

        assertFalse(service.verify(forged).isPresent());
    }

    @Test
    @DisplayName("A token signed with a different secret is rejected")
    void otherSecret_isRejected() {
        UnsubscribeTokenService other = new UnsubscribeTokenService(true, "fedcba9876543210fedcba9876543210");

        assertFalse(service.verify(other.issue(42L, Kind.EVENTS)).isPresent());
    }

    @Test
    @DisplayName("Garbage is rejected quietly, never with an exception")
    void garbage_isRejected() {
        assertFalse(service.verify(null).isPresent());
        assertFalse(service.verify("").isPresent());
        assertFalse(service.verify("not-base64-!!!").isPresent());
        assertFalse(service.verify(Base64.getUrlEncoder().encodeToString("42:EVENTS:zz".getBytes())).isPresent());
        assertFalse(service.verify(Base64.getUrlEncoder().encodeToString("no-colons".getBytes())).isPresent());
    }

    @Test
    @DisplayName("With mail enabled and no secret, the application refuses to start")
    void mailEnabledWithoutSecret_failsToStart() {
        new ApplicationContextRunner()
                .withBean(UnsubscribeTokenService.class)
                .withPropertyValues("cpclub.mail.enabled=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("With mail enabled, a short secret is refused too")
    void mailEnabledWithShortSecret_failsToStart() {
        new ApplicationContextRunner()
                .withBean(UnsubscribeTokenService.class)
                .withPropertyValues("cpclub.mail.enabled=true", "cpclub.mail.unsubscribe-secret=too-short")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    @DisplayName("With mail disabled, no secret is needed to start")
    void mailDisabledWithoutSecret_starts() {
        new ApplicationContextRunner()
                .withBean(UnsubscribeTokenService.class)
                .withPropertyValues("cpclub.mail.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
