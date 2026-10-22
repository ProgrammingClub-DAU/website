package com.cpclub.backend.sync.entity;

/** Identifies the type of sync run recorded in sync_runs. */
public enum SyncJob {
    /** Codeforces current and peak rating (user.info). Drives the main leaderboard. */
    CF_RATINGS,
    CF_SUBMISSIONS,
    CF_PROBLEMSET,
    CF_CONTESTS,
    /** LeetCode contest rating and solved totals. Drives the LeetCode leaderboard. */
    LC_RATINGS,
    LC_CONTESTS,
    LC_TOTALS,
    EXTERNAL_CONTESTS,
    CLUB_CONTEST
}
