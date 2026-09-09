package com.cpclub.backend.leaderboard.dto;

/**
 * Which platform's rating the leaderboard ranks by.
 *
 * <p>An enum rather than a raw query string so an unrecognized value is rejected
 * as a 400 by parameter binding, before it can reach the repository. The value is
 * interpolated into a native query; keeping the set closed is what makes that
 * safe.</p>
 */
public enum LeaderboardPlatform {

    /** Ranks by {@code users.rating}, synced from Codeforces. The default. */
    CODEFORCES,

    /** Ranks by {@code users.leetcode_rating}, synced from LeetCode. */
    LEETCODE
}
