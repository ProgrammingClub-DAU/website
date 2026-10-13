package com.cpclub.backend.stats.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Resolved UTC start/end boundaries and IST start date for a period (D1, D31).
 */
public record StatsPeriodResolved(
        Instant startUtc,
        Instant endUtc,
        LocalDate startDateIst
) {}
