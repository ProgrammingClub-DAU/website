package com.cpclub.backend.halloffame;

import com.cpclub.backend.halloffame.dto.HallOfFameEntryRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a Hall of Fame entry may contain.
 *
 * <p>The URL rules are the ones that matter. Links and photos are rendered into
 * {@code href} and {@code src}, and a {@code javascript:} URL in an {@code href}
 * runs in whoever clicks it. The request is the only place those strings enter,
 * so it is the only place worth checking.</p>
 */
class HallOfFameEntryRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(document.cookie)",
            "JAVASCRIPT:alert(1)",
            "data:text/html,<script>alert(1)</script>",
            "vbscript:msgbox(1)",
            "//evil.example/path",
            "ftp://files.example/x",
            "https://example.com/with space"
    })
    @DisplayName("A link that is not a plain http(s) URL is refused")
    void rejectsNonWebLinks(String url) {
        assertFalse(isValid(withLink(url)), "accepted: " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:image/png;base64,AAAA"})
    @DisplayName("A photo that is not a plain http(s) URL is refused")
    void rejectsNonWebImages(String url) {
        assertFalse(isValid(withPhoto(url)), "accepted: " + url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://icpc.global/regionals/finder/Amritapuri-2025",
            "http://codeforces.com/contest/1234/standings"
    })
    @DisplayName("Ordinary web links are accepted")
    void acceptsWebLinks(String url) {
        assertTrue(isValid(withLink(url)));
    }

    @Test
    @DisplayName("An entry needs a heading and a date")
    void requiresHeadingAndDate() {
        assertFalse(isValid(new HallOfFameEntryRequest(" ", null, null, LocalDate.now(), null, null)));
        assertFalse(isValid(new HallOfFameEntryRequest("Heading", null, null, null, null, null)));
    }

    @Test
    @DisplayName("Links and photos are optional")
    void childrenAreOptional() {
        assertTrue(isValid(new HallOfFameEntryRequest("Heading", null, null, LocalDate.now(), null, null)));
    }

    @Test
    @DisplayName("An entry cannot carry more than ten links")
    void capsTheNumberOfLinks() {
        List<HallOfFameEntryRequest.Link> eleven = Collections.nCopies(11,
                new HallOfFameEntryRequest.Link("Link", "https://example.com"));
        assertFalse(isValid(new HallOfFameEntryRequest("Heading", null, null, LocalDate.now(), eleven, null)));
    }

    private HallOfFameEntryRequest withLink(String url) {
        return new HallOfFameEntryRequest("Heading", null, null, LocalDate.now(),
                List.of(new HallOfFameEntryRequest.Link("Link", url)), null);
    }

    private HallOfFameEntryRequest withPhoto(String url) {
        return new HallOfFameEntryRequest("Heading", null, null, LocalDate.now(),
                null, List.of(new HallOfFameEntryRequest.Photo(url, null)));
    }

    private boolean isValid(HallOfFameEntryRequest request) {
        Set<ConstraintViolation<HallOfFameEntryRequest>> violations = validator.validate(request);
        return violations.isEmpty();
    }
}
