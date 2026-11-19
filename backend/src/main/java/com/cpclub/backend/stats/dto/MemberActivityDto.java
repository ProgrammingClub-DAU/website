package com.cpclub.backend.stats.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A member's recent activity: problems solved per day and their latest solves.
 * Public, like the rest of /api/stats/users/{id}; also part of the admin's CP
 * Practice Group student page.
 *
 * @param daily        the last 30 IST days, oldest first
 * @param recentSolves the latest Codeforces solves, newest first
 */
public record MemberActivityDto(List<DailySolves> daily, List<RecentSolve> recentSolves) {

    /**
     * Problems solved on one IST date. Codeforces counts distinct problems from
     * regular contests by first accepted submission; LeetCode is the rise in the
     * total from the day before, null where either day's snapshot is missing.
     */
    public record DailySolves(LocalDate date, int codeforces, Integer leetcode) {}

    /** @param solvedAt UTC, the first accepted submission */
    public record RecentSolve(String problemKey, String name, Integer rating, LocalDateTime solvedAt, String url) {}
}
