package com.cpclub.backend.sync.entity;

/** Identifies the type of sync run recorded in sync_runs. */
public enum SyncJob {
    CF_SUBMISSIONS,
    CF_PROBLEMSET,
    CF_CONTESTS,
    LC_CONTESTS,
    LC_TOTALS,
    EXTERNAL_CONTESTS,
    CLUB_CONTEST
}