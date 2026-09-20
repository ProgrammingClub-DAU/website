package com.cpclub.backend.leaderboard.dto;

import com.cpclub.backend.user.entity.User;

/**
 * Immutable DTO record representing a single ranked entry on the leaderboard.
 * Displays calculated Codeforces performance tiers and dynamic banner status.
 */
public record LeaderboardResponseDto(
        int rank,
        Long userId,
        String name,
        String codeforcesHandle,
        Integer rating,
        String tier,
        String clubRole,
        String avatarUrl,
        String equippedBannerId,
        String rankBannerId,
        String activeBannerId,
        Integer maxRating,
        boolean isPlatformCreator
) {
    /** Backwards-compatible constructor for existing callers and tests. */
    public LeaderboardResponseDto(
            int rank,
            Long userId,
            String name,
            String codeforcesHandle,
            Integer rating,
            String tier,
            String clubRole,
            String avatarUrl
    ) {
        this(
                rank,
                userId,
                name,
                codeforcesHandle,
                rating,
                tier,
                clubRole,
                avatarUrl,
                "rookie",
                calculateRankBanner(rank),
                calculateActiveBanner(rank, "rookie"),
                rating,
                false
        );
    }

    public static String calculateRankBanner(int rank) {
        return switch (rank) {
            case 1 -> "rank-gold";
            case 2 -> "rank-silver";
            case 3 -> "rank-bronze";
            default -> null;
        };
    }

    public static String calculateActiveBanner(int rank, String equippedBannerId) {
        String rankBanner = calculateRankBanner(rank);
        if (rankBanner != null) return rankBanner;
        return (equippedBannerId != null && !equippedBannerId.isBlank()) ? equippedBannerId : "rookie";
    }

    /**
     * Resolves the official Codeforces rating tier based on rating thresholds.
     *
     * @param rating numerical Codeforces rating
     * @return String representation of Codeforces tier title
     */
    public static String calculateTier(Integer rating) {
        if (rating == null) return "Unrated";
        if (rating >= 3000) return "Legendary Grandmaster";
        if (rating >= 2600) return "International Grandmaster";
        if (rating >= 2400) return "Grandmaster";
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
        String rankBanner = calculateRankBanner(rank);
        String equipped = user.getEquippedBannerId() != null ? user.getEquippedBannerId() : "rookie";
        Integer maxR = user.getMaxRating() != null ? user.getMaxRating() : user.getRating();
        return new LeaderboardResponseDto(
                rank,
                user.getId(),
                user.getName(),
                user.getCodeforcesHandle(),
                user.getRating(),
                calculateTier(user.getRating()),
                user.getClubRole() != null ? user.getClubRole().name() : null,
                user.getAvatarUrl(),
                equipped,
                rankBanner,
                rankBanner != null ? rankBanner : equipped,
                maxR,
                user.isPlatformCreator()
        );
    }

    /**
     * Maps a database-ranked row into an immutable leaderboard entry.
     *
     * @param row projection carrying the member and its computed placement
     * @return public ranking entry with a calculated Codeforces tier
     */
    public static LeaderboardResponseDto fromProjection(LeaderboardEntryProjection row) {
        int rank = row.getPlacement().intValue();
        String rankBanner = calculateRankBanner(rank);
        String equipped = row.getEquippedbannerid() != null ? row.getEquippedbannerid() : "rookie";
        Integer maxR = row.getMaxrating() != null ? row.getMaxrating() : row.getRating();
        return new LeaderboardResponseDto(
                rank,
                row.getId(),
                row.getName(),
                row.getHandle(),
                row.getRating(),
                calculateTier(row.getRating()),
                row.getClubrole(),
                row.getAvatarurl(),
                equipped,
                rankBanner,
                rankBanner != null ? rankBanner : equipped,
                maxR,
                row.getIsplatformcreator() != null ? row.getIsplatformcreator() : false
        );
    }
}
