package com.cpclub.backend.auth.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter for verification email resends (Section 4.11, D30).
 *
 * <p>Enforces two thresholds per member:
 * <ul>
 *   <li>Maximum 1 request per 60 seconds</li>
 *   <li>Maximum 5 requests per 24-hour window</li>
 * </ul>
 * </p>
 */
@Component
public class ResendRateLimiter {

    private final Map<Long, RateState> userStates = new ConcurrentHashMap<>();

    private static class RateState {
        Instant lastSent = Instant.EPOCH;
        Instant dayWindowStart = Instant.EPOCH;
        int countToday = 0;
    }

    /**
     * Checks if the member is rate-limited. If not, records the attempt and returns 0.
     * If rate-limited, returns the suggested Retry-After value in seconds (> 0).
     *
     * @param userId member user ID
     * @return 0 if allowed, otherwise seconds to wait
     */
    public synchronized long tryAcquireOrGetRetryAfter(Long userId) {
        Instant now = Instant.now();
        RateState state = userStates.computeIfAbsent(userId, id -> new RateState());

        // Check if 24 hours have elapsed to reset the daily window
        if (Duration.between(state.dayWindowStart, now).toHours() >= 24) {
            state.dayWindowStart = now;
            state.countToday = 0;
        }

        // Check 60-second cooldown
        long secondsSinceLast = Duration.between(state.lastSent, now).toSeconds();
        if (secondsSinceLast < 60) {
            return Math.max(1, 60 - secondsSinceLast);
        }

        // Check 5 requests per day limit
        if (state.countToday >= 5) {
            long remainingDaySeconds = Duration.between(now, state.dayWindowStart.plus(Duration.ofDays(1))).toSeconds();
            return Math.max(1, remainingDaySeconds);
        }

        // Allow and record
        state.lastSent = now;
        if (state.countToday == 0) {
            state.dayWindowStart = now;
        }
        state.countToday++;
        return 0;
    }

    public void clear() {
        userStates.clear();
    }
}
