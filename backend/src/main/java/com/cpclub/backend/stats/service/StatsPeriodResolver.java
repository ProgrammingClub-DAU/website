package com.cpclub.backend.stats.service;

import com.cpclub.backend.stats.dto.StatsPeriod;
import com.cpclub.backend.stats.dto.StatsPeriodResolved;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Resolves calendar period boundaries in Asia/Kolkata and converts them to UTC (D1, D31).
 * Uses an injected Clock bean so tests can deterministically pin the reference time.
 */
@Component
@RequiredArgsConstructor
public class StatsPeriodResolver {

    private final Clock clock;
    public static final ZoneId ZONE_IST = ZoneId.of("Asia/Kolkata");

    /**
     * Resolves start/end UTC instants and the IST start calendar date for a period.
     *
     * @param period requested calendar period
     * @return resolved UTC boundaries and IST start date
     */
    public StatsPeriodResolved resolve(StatsPeriod period) {
        if (period == null) {
            period = StatsPeriod.ALL_TIME;
        }

        Instant nowUtc = clock.instant();
        ZonedDateTime nowIst = nowUtc.atZone(ZONE_IST);

        return switch (period) {
            case WEEK -> {
                ZonedDateTime startIst = nowIst.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        .truncatedTo(ChronoUnit.DAYS);
                yield new StatsPeriodResolved(startIst.toInstant(), nowUtc, startIst.toLocalDate());
            }
            case MONTH -> {
                ZonedDateTime startIst = nowIst.withDayOfMonth(1)
                        .truncatedTo(ChronoUnit.DAYS);
                yield new StatsPeriodResolved(startIst.toInstant(), nowUtc, startIst.toLocalDate());
            }
            case YEAR -> {
                ZonedDateTime startIst = nowIst.withDayOfYear(1)
                        .truncatedTo(ChronoUnit.DAYS);
                yield new StatsPeriodResolved(startIst.toInstant(), nowUtc, startIst.toLocalDate());
            }
            case ALL_TIME -> new StatsPeriodResolved(
                    Instant.EPOCH,
                    nowUtc,
                    LocalDate.of(1970, 1, 1)
            );
        };
    }
}
