package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Immutable subset of a Codeforces user record needed by the club leaderboard.
 *
 * @param handle Codeforces account handle
 * @param rating current contest rating, or {@code null} for unrated accounts
 * @param maxRating highest historical rating
 * @param rank current Codeforces rank name
 * @param maxRank highest historical Codeforces rank name
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodeforcesUserDto(
        String handle,
        Integer rating,
        Integer maxRating,
        String rank,
        String maxRank
) {
}
