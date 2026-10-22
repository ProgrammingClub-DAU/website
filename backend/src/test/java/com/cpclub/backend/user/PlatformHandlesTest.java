package com.cpclub.backend.user;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.user.service.PlatformHandles;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformHandlesTest {

    @ParameterizedTest
    @CsvSource({
            "https://codeforces.com/profile/Ishaan-K, Ishaan-K",
            "http://www.codeforces.com/profile/tourist/, tourist",
            "codeforces.com/profile/jiangly?locale=en, jiangly",
            "https://m1.codeforces.com/submissions/Petr, Petr",
            "'  @tourist  ', tourist",
            "Um_nik, Um_nik",
            "a.b-c_d, a.b-c_d"
    })
    void codeforcesInputIsReducedToTheHandle(String raw, String expected) {
        assertThat(PlatformHandles.normalizeCodeforces(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/tourist", "has spaces", "ab", "xxxxxxxxxxxxxxxxxxxxxxxxx", "bad!char"})
    void codeforcesNonHandlesAreRefused(String raw) {
        assertThatThrownBy(() -> PlatformHandles.normalizeCodeforces(raw)).isInstanceOf(BadRequestException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "https://leetcode.com/u/neal_wu/, neal_wu",
            "leetcode.com/lee215, lee215",
            "@votrubac, votrubac"
    })
    void leetcodeInputIsReducedToTheUsername(String raw, String expected) {
        assertThat(PlatformHandles.normalizeLeetcode(raw)).isEqualTo(expected);
    }

    @Test
    void leetcodeLinksThatAreNotProfilesAreRefused() {
        assertThatThrownBy(() -> PlatformHandles.normalizeLeetcode("https://leetcode.com/problems/two-sum/"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void blankMeansNotSupplied() {
        assertThat(PlatformHandles.normalizeCodeforces("   ")).isNull();
        assertThat(PlatformHandles.normalizeLeetcode(null)).isNull();
    }
}
