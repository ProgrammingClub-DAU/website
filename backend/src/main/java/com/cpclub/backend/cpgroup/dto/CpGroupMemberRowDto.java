package com.cpclub.backend.cpgroup.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One student in the CP Practice Group, with their recent activity.
 *
 * <p>Solve counts follow the leaderboard's rules: Codeforces counts distinct
 * problems from regular contests (no gym) by first accepted submission; LeetCode
 * counts the change in the member's total since that day's snapshot, and is null
 * where there is no snapshot that far back (tracking began later) or no handle.
 * Windows are rolling: the last 7 and 30 days.</p>
 *
 * @param batchYear        the group's batch, set by an admin
 * @param profileBatchYear the batch on the student's own profile, for reference
 * @param addedAt          UTC
 * @param lastActiveOn     the latest IST date with a Codeforces solve, a rise in the
 *                         LeetCode total, or a contest; null when none is known
 */
public record CpGroupMemberRowDto(
        Long userId,
        String name,
        String email,
        String avatarUrl,
        String codeforcesHandle,
        String leetcodeHandle,
        Integer batchYear,
        Integer profileBatchYear,
        LocalDateTime addedAt,
        int cfSolved7d,
        int cfSolved30d,
        int cfSolvedAll,
        Integer lcSolved7d,
        Integer lcSolved30d,
        Integer lcSolvedAll,
        Integer cfRating,
        Integer cfRatingChange30d,
        Integer lcRating,
        Integer lcRatingChange30d,
        int contests30d,
        LocalDate lastActiveOn,
        boolean hasNotes) {
}
