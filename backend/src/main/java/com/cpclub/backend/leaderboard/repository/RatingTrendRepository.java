package com.cpclub.backend.leaderboard.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Each member's most recent rated contests, for the leaderboard's trend line and
 * last rating change.
 *
 * <p>The leaderboard used to draw both from numbers made up out of the member's id
 * -- every row showed a sparkline and a "+23" or "-15" that had never happened.</p>
 */
@Repository
@RequiredArgsConstructor
public class RatingTrendRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /**
     * @param recentRatings new rating after each of the last few rated contests, oldest first
     * @param lastChange    rating change in the most recent rated contest, or null when unknown
     */
    public record Trend(List<Integer> recentRatings, Integer lastChange) {}

    /**
     * One query for a whole page of members.
     *
     * @param platform {@code CODEFORCES} or {@code LEETCODE}
     * @param points   how many contests per member
     */
    public Map<Long, Trend> findRecentTrends(Collection<Long> userIds, String platform, int points) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        String sql = """
                SELECT user_id, new_rating, old_rating, rn
                FROM (
                    SELECT cp.user_id, cp.new_rating, cp.old_rating,
                           ROW_NUMBER() OVER (PARTITION BY cp.user_id ORDER BY cp.started_at DESC, cp.id DESC) AS rn
                    FROM contest_participations cp
                    WHERE cp.user_id IN (:ids)
                      AND cp.platform = :platform
                      AND cp.rated = true
                      AND cp.new_rating IS NOT NULL
                ) recent
                WHERE rn <= :points
                ORDER BY user_id, rn DESC
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", userIds)
                .addValue("platform", platform)
                .addValue("points", points);

        Map<Long, List<Integer>> ratings = new HashMap<>();
        Map<Long, Integer> lastChange = new HashMap<>();
        jdbc.query(sql, params, rs -> {
            long userId = rs.getLong("user_id");
            int newRating = rs.getInt("new_rating");
            ratings.computeIfAbsent(userId, k -> new ArrayList<>()).add(newRating);
            if (rs.getInt("rn") == 1) {
                Object old = rs.getObject("old_rating");
                if (old != null) {
                    lastChange.put(userId, newRating - ((Number) old).intValue());
                }
            }
        });

        Map<Long, Trend> trends = new HashMap<>();
        ratings.forEach((userId, list) -> trends.put(userId, new Trend(List.copyOf(list), lastChange.get(userId))));
        return trends;
    }
}
