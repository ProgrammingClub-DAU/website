package com.cpclub.backend.leaderboard.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Backend definition and validation catalog for member leaderboard banners.
 */
public record BannerConfig(
        String id,
        String name,
        int minRating,
        String rarity,
        String colors,
        String animation
) {
    public static final Map<String, BannerConfig> RATING_BANNERS;

    static {
        Map<String, BannerConfig> map = new LinkedHashMap<>();
        map.put("rookie", new BannerConfig("rookie", "Rookie", 0, "Common", "#2a2d3a,#4a4f66", "geometric"));
        map.put("pupil", new BannerConfig("pupil", "Pupil", 1200, "Common", "#173a2a,#2f7d58", "waves"));
        map.put("specialist", new BannerConfig("specialist", "Specialist", 1400, "Rare", "#123a44,#2a8ea6", "waves"));
        map.put("expert", new BannerConfig("expert", "Expert", 1600, "Rare", "#1a2f5a,#3f6fd1", "circuits"));
        map.put("candidate-master", new BannerConfig("candidate-master", "Candidate Master", 1900, "Epic", "#3a2a6a,#8a6cf0", "particles"));
        map.put("master", new BannerConfig("master", "Master", 2100, "Legendary", "#5a3210,#f0a040", "flames"));
        map.put("club-founder", new BannerConfig("club-founder", "Club Founder", 0, "Special", "#401828,#e04888", "shimmer"));
        map.put("contest-winner", new BannerConfig("contest-winner", "Contest Winner", 0, "Special", "#302040,#d09030", "sparks"));
        map.put("creator-vip", new BannerConfig("creator-vip", "Website Architect (VIP)", 0, "Exclusive", "#2e0508,#ff1e42", "flames"));
        RATING_BANNERS = Collections.unmodifiableMap(map);
    }

    public static boolean isValidRatingBanner(String bannerId) {
        return bannerId != null && RATING_BANNERS.containsKey(bannerId.toLowerCase().trim());
    }

    public static BannerConfig getBanner(String bannerId) {
        if (bannerId == null) return RATING_BANNERS.get("rookie");
        return RATING_BANNERS.getOrDefault(bannerId.toLowerCase().trim(), RATING_BANNERS.get("rookie"));
    }
}
