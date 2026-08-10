package com.cpclub.backend.leaderboard.dto;

import com.cpclub.backend.user.entity.User;

/**
 * Immutable DTO record representing a single ranked entry on the leaderboard.
 * Displays calculated Codeforces performance tiers.
 */
public record LeaderboardResponseDto(
        int rank,
        Long userId,
        String name,
        String codeforcesHandle,
        Integer rating,
        String tier
) {
    /**
     * Resolves the official Codeforces rating tier based on rating thresholds.
     *
     * @param rating numerical Codeforces rating
     * @return String representation of Codeforces tier title
     */
    public static String calculateTier(Integer rating) {
        if (rating == null) return "Unrated";
        if (rating >= 3000) return "Legendary Grandmaster";
        if (rating >= 2600) return "Grandmaster";
        if (rating >= 2400) return "International Grandmaster";
        if (rating >= 2300) return "International Master";
        if (rating >= 2100) return "Master";
        if (rating >= 1900) return "Candidate Master";
        if (rating >= 1600) return "Expert";
        if (rating >= 1400) return "Specialist";
        if (rating >= 1200) return "Pupil";
        return "Newbie";
    }

    /**
     * Maps a member and its calculated placement into an immutable leaderboard entry.
     *
     * @param user member whose synchronized rating is displayed
     * @param rank one-based position within the full leaderboard
     * @return public ranking entry with a calculated Codeforces tier
     */
    public static LeaderboardResponseDto fromEntity(User user, int rank) {
        return new LeaderboardResponseDto(
                rank,
                user.getId(),
                user.getName(),
                user.getCodeforcesHandle(),
                user.getRating(),
                calculateTier(user.getRating())
        );
    }
}
