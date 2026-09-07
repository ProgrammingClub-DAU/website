package com.cpclub.backend.leaderboard.dto;

/**
 * Which slice of the membership the leaderboard shows.
 *
 * <p>An enum rather than a raw query string so an unrecognized value is rejected
 * as a 400 by parameter binding, before it can reach the repository.</p>
 *
 * <p>The groupings are club positions, not authorization roles — see
 * {@link com.cpclub.backend.user.entity.ClubRole}. Filtering to {@code CORE} does
 * not select the admins.</p>
 */
public enum LeaderboardFilter {

    /** Everyone. The default. */
    ALL,

    /** The core team and the two convenor posts, which run the club together. */
    CORE,

    /** Batch representatives. */
    BATCH_REP,

    /**
     * Ordinary members.
     *
     * <p>Includes members with no club position recorded at all. Every account
     * created before Phase 2 has a null {@code club_role}, so a definition of
     * "student" that only matched the explicit {@code STUDENT} value would show
     * an empty board on the day this shipped.</p>
     */
    STUDENTS
}
