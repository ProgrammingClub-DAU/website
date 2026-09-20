package com.cpclub.backend.calendar.entity;

/**
 * Platforms whose contest schedules are fetched for the external calendar (D26).
 * Includes CODECHEF and ATCODER for schedule display only -- their member stats
 * remain link-only per D9.
 */
public enum ContestPlatform {
    CODEFORCES,
    LEETCODE,
    CODECHEF,
    ATCODER
}