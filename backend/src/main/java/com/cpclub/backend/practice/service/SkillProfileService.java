package com.cpclub.backend.practice.service;

import com.cpclub.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cpclub.backend.practice.service.TopicStrengthService.CORE_TAGS;

/**
 * Estimates a member's Codeforces level overall and in each core topic, from the
 * ratings of the problems they have solved.
 *
 * <p>A level is the mean of the hardest rated problems solved: the ten hardest
 * overall, the five hardest in a topic. Only the hardest count, so a pile of easy
 * problems does not make a topic look strong. A missing slot -- fewer than five
 * solves in a topic -- counts as {@link #UNTOUCHED_TOPIC_GAP} below the overall
 * level, so a topic with few solves reads as weak, and solving an easy problem
 * never lowers a level. With a contest rating, the overall level is that rating,
 * raised halfway toward the practice level when the member solves harder
 * problems than they are rated for.</p>
 *
 * <p>The focus topics are the three with the lowest levels among those common
 * near the member's level ({@link #MIN_TOPIC_SHARE} of the problems there), so a
 * rare topic such as geometry is not everyone's weakest. They are only picked
 * once there are {@link #MIN_SOLVES_FOR_TOPICS} rated solves to go on.</p>
 */
@Service
@RequiredArgsConstructor
public class SkillProfileService {

    static final int FLOOR = 800;
    static final int OVERALL_SAMPLE = 10;
    static final int TOPIC_SAMPLE = 5;
    static final int UNTOUCHED_TOPIC_GAP = 400;
    public static final int MIN_SOLVES_FOR_TOPICS = 30;
    static final double MIN_TOPIC_SHARE = 0.04;
    static final int FOCUS_TOPICS = 3;

    private final NamedParameterJdbcTemplate jdbc;

    public SkillProfile profileOf(User user) {
        Set<Long> seen = new HashSet<>();
        List<Integer> solved = new ArrayList<>();
        Map<String, List<Integer>> solvedByTag = new HashMap<>();
        jdbc.query("""
                SELECT p.id, p.rating, pt.tag
                FROM cf_solves s
                JOIN cf_problems p ON p.id = s.problem_id
                LEFT JOIN cf_problem_tags pt ON pt.problem_id = p.id AND pt.tag IN (:coreTags)
                WHERE s.user_id = :userId
                  AND p.rating IS NOT NULL
                  AND (p.contest_id IS NULL OR p.contest_id < 100000)
                """,
                new MapSqlParameterSource("userId", user.getId()).addValue("coreTags", CORE_TAGS),
                rs -> {
                    int rating = rs.getInt("rating");
                    if (seen.add(rs.getLong("id"))) solved.add(rating);
                    String tag = rs.getString("tag");
                    if (tag != null) solvedByTag.computeIfAbsent(tag, t -> new ArrayList<>()).add(rating);
                });

        int overall = overallLevel(user.getRating(), solved);
        Map<String, Integer> topicLevels = new LinkedHashMap<>();
        Map<String, Integer> topicSolves = new LinkedHashMap<>();
        for (String tag : CORE_TAGS) {
            List<Integer> ratings = solvedByTag.getOrDefault(tag, List.of());
            topicLevels.put(tag, topicLevel(ratings, overall));
            topicSolves.put(tag, ratings.size());
        }
        List<String> weak = solved.size() < MIN_SOLVES_FOR_TOPICS
                ? List.of()
                : weakTags(topicLevels, topicSharesNear(overall));
        return new SkillProfile(overall, topicLevels, topicSolves, solved.size(), weak);
    }

    static int overallLevel(Integer cfRating, List<Integer> solvedRatings) {
        int practice = sampleLevel(solvedRatings, OVERALL_SAMPLE, FLOOR);
        if (cfRating == null || cfRating <= 0) return practice;
        return roundTo10(Math.max(cfRating, (cfRating + practice) / 2.0));
    }

    static int topicLevel(List<Integer> ratings, int overallLevel) {
        return sampleLevel(ratings, TOPIC_SAMPLE, Math.max(FLOOR, overallLevel - UNTOUCHED_TOPIC_GAP));
    }

    /** The mean of the {@code size} hardest ratings, each counted as at least {@code floor}; missing ones count as {@code floor}. */
    static int sampleLevel(List<Integer> ratings, int size, int floor) {
        List<Integer> hardest = ratings.stream().sorted(Comparator.reverseOrder()).limit(size).toList();
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += i < hardest.size() ? Math.max(hardest.get(i), floor) : floor;
        }
        return roundTo10(sum / size);
    }

    /**
     * The lowest-level topics among those common near the member's level, weakest
     * first; ties go to the more common topic. With no share data (an empty
     * problem table) every topic counts as common.
     */
    static List<String> weakTags(Map<String, Integer> topicLevels, Map<String, Double> shareNearLevel) {
        return CORE_TAGS.stream()
                .filter(tag -> shareNearLevel.isEmpty() || shareNearLevel.getOrDefault(tag, 0.0) >= MIN_TOPIC_SHARE)
                .sorted(Comparator.comparingInt((String tag) -> topicLevels.get(tag))
                        .thenComparing(tag -> shareNearLevel.getOrDefault(tag, 0.0), Comparator.reverseOrder())
                        .thenComparing(Comparator.naturalOrder()))
                .limit(FOCUS_TOPICS)
                .toList();
    }

    /** For each core tag, the share of problems rated near this level that carry it. */
    private Map<String, Double> topicSharesNear(int level) {
        MapSqlParameterSource band = new MapSqlParameterSource()
                .addValue("lo", level - 200)
                .addValue("hi", level + 300)
                .addValue("coreTags", CORE_TAGS);
        Integer total = jdbc.queryForObject("""
                SELECT COUNT(*) FROM cf_problems p
                WHERE p.rating BETWEEN :lo AND :hi AND (p.contest_id IS NULL OR p.contest_id < 100000)
                """, band, Integer.class);
        Map<String, Double> shares = new HashMap<>();
        if (total == null || total == 0) return shares;
        jdbc.query("""
                SELECT pt.tag, COUNT(*) AS n
                FROM cf_problems p JOIN cf_problem_tags pt ON pt.problem_id = p.id
                WHERE p.rating BETWEEN :lo AND :hi AND (p.contest_id IS NULL OR p.contest_id < 100000)
                  AND pt.tag IN (:coreTags)
                GROUP BY pt.tag
                """, band, rs -> { shares.put(rs.getString("tag"), rs.getInt("n") / (double) total); });
        return shares;
    }

    private static int roundTo10(double value) {
        return (int) Math.round(value / 10.0) * 10;
    }
}
