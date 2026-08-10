package com.cpclub.backend.leaderboard.dto;

import com.cpclub.backend.user.entity.User;

public record LeaderboardResponseDto(
        int rank,
        Long userId,
        String name,
        String codeforcesHandle,
        Integer rating,
        String tier
) {
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
