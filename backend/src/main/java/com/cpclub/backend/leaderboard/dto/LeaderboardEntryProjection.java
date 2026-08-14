package com.cpclub.backend.leaderboard.dto;

/**
 * Read-only projection of one ranked leaderboard row, populated directly by the
 * database.
 *
 * <p>The rank arrives as a column rather than being computed in Java, so a page
 * of results costs exactly one query no matter how many members it contains.</p>
 *
 * <p>Every accessor maps to a single lowercase column alias in
 * {@code UserRepository.findLeaderboardPage} — no underscores and no camel case.
 * PostgreSQL and H2 both fold unquoted aliases to lowercase, so single-word
 * aliases are the one form that resolves identically on both.</p>
 */
public interface LeaderboardEntryProjection {

    /** @return member identifier, aliased {@code id} */
    Long getId();

    /** @return member display name, aliased {@code name} */
    String getName();

    /** @return linked Codeforces handle or {@code null}, aliased {@code handle} */
    String getHandle();

    /** @return last synchronized rating or {@code null}, aliased {@code rating} */
    Integer getRating();

    /**
     * One-based position across the whole leaderboard, not just the current page.
     *
     * <p>Aliased {@code placement} rather than {@code rank} or {@code position}:
     * both of those are reserved words in SQL.</p>
     *
     * @return absolute rank, where tied ratings share a value
     */
    Long getPlacement();
}
