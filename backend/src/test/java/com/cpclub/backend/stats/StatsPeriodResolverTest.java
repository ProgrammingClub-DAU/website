package com.cpclub.backend.stats.service;

import com.cpclub.backend.stats.dto.StatsPeriod;
import com.cpclub.backend.stats.dto.StatsPeriodResolved;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class StatsPeriodResolverTest {

    private ZoneId istZone;

    @BeforeEach
    void setUp() {
        istZone = ZoneId.of("Asia/Kolkata");
    }

    @Test
    void weekBoundary_excludesSundayNightIst() {
        // Wednesday, 2026-04-15 at 12:00:00 IST
        // The Monday of this week in IST is 2026-04-13.
        // Previous Sunday night is 2026-04-12 23:59:59 IST.
        Instant nowUtc = Instant.parse("2026-04-15T06:30:00Z"); // 12:00:00 IST
        Clock clock = Clock.fixed(nowUtc, istZone);
        StatsPeriodResolver resolver = new StatsPeriodResolver(clock);

        StatsPeriodResolved week = resolver.resolve(StatsPeriod.WEEK);

        // Start in IST should be 2026-04-13 00:00:00 IST -> 2026-04-12 18:30:00 UTC
        Instant expectedStartUtc = Instant.parse("2026-04-12T18:30:00Z");
        assertThat(week.startUtc()).isEqualTo(expectedStartUtc);
        assertThat(week.startDateIst()).isEqualTo(LocalDate.of(2026, 4, 13));

        // Sunday night 23:59:59 IST is 2026-04-12 18:29:59 UTC
        Instant sundayNightIstInUtc = Instant.parse("2026-04-12T18:29:59Z");
        assertThat(sundayNightIstInUtc).isBefore(week.startUtc());
    }

    @Test
    void weekBoundary_includesMondayMidnightIst() {
        Instant nowUtc = Instant.parse("2026-04-15T06:30:00Z");
        Clock clock = Clock.fixed(nowUtc, istZone);
        StatsPeriodResolver resolver = new StatsPeriodResolver(clock);

        StatsPeriodResolved week = resolver.resolve(StatsPeriod.WEEK);

        // Monday 00:00:00 IST is 2026-04-12 18:30:00 UTC
        Instant mondayMidnightIstInUtc = Instant.parse("2026-04-12T18:30:00Z");
        assertThat(mondayMidnightIstInUtc).isEqualTo(week.startUtc());
        // Condition in queries is >= startUtc and < endUtc
        assertThat(mondayMidnightIstInUtc.isBefore(week.startUtc())).isFalse();
        assertThat(mondayMidnightIstInUtc.isBefore(week.endUtc())).isTrue();
    }

    @Test
    void monthBoundary_resolvesFirstOfIstMonth() {
        Instant nowUtc = Instant.parse("2026-04-15T06:30:00Z");
        Clock clock = Clock.fixed(nowUtc, istZone);
        StatsPeriodResolver resolver = new StatsPeriodResolver(clock);

        StatsPeriodResolved month = resolver.resolve(StatsPeriod.MONTH);

        // 2026-04-01 00:00:00 IST -> 2026-03-31 18:30:00 UTC
        Instant expectedStartUtc = Instant.parse("2026-03-31T18:30:00Z");
        assertThat(month.startUtc()).isEqualTo(expectedStartUtc);
        assertThat(month.startDateIst()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void yearBoundary_resolvesJanFirstOfIstYear() {
        Instant nowUtc = Instant.parse("2026-04-15T06:30:00Z");
        Clock clock = Clock.fixed(nowUtc, istZone);
        StatsPeriodResolver resolver = new StatsPeriodResolver(clock);

        StatsPeriodResolved year = resolver.resolve(StatsPeriod.YEAR);

        // 2026-01-01 00:00:00 IST -> 2025-12-31 18:30:00 UTC
        Instant expectedStartUtc = Instant.parse("2025-12-31T18:30:00Z");
        assertThat(year.startUtc()).isEqualTo(expectedStartUtc);
        assertThat(year.startDateIst()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void allTimeBoundary_resolvesToEpochAnd1970() {
        Instant nowUtc = Instant.parse("2026-04-15T06:30:00Z");
        Clock clock = Clock.fixed(nowUtc, istZone);
        StatsPeriodResolver resolver = new StatsPeriodResolver(clock);

        StatsPeriodResolved allTime = resolver.resolve(StatsPeriod.ALL_TIME);

        assertThat(allTime.startUtc()).isEqualTo(Instant.EPOCH);
        assertThat(allTime.startDateIst()).isEqualTo(LocalDate.of(1970, 1, 1));
        assertThat(allTime.endUtc()).isEqualTo(nowUtc);
    }
}
