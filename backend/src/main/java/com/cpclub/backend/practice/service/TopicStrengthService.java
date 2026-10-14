package com.cpclub.backend.practice.service;

import com.cpclub.backend.practice.dto.TopicStrengthDto;
import com.cpclub.backend.practice.dto.TopicStrengthResponseDto;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for calculating member topic strength across 18 core Codeforces tags and an other bucket (Section 1.4, 6.5).
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TopicStrengthService {

    public static final List<String> CORE_TAGS = List.of(
            "dp",
            "greedy",
            "math",
            "graphs",
            "implementation",
            "constructive algorithms",
            "binary search",
            "sortings",
            "number theory",
            "strings",
            "trees",
            "dfs and similar",
            "brute force",
            "data structures",
            "two pointers",
            "bitmasks",
            "combinatorics",
            "geometry"
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public TopicStrengthResponseDto getUserTopicStrength(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            return TopicStrengthResponseDto.builder()
                    .userId(userId)
                    .codeforcesHandle(user != null ? user.getCodeforcesHandle() : null)
                    .reason("NO_CODEFORCES_HANDLE")
                    .totalCountedSolves(0)
                    .topics(List.of())
                    .build();
        }

        // 1. Total counted solves for member (D3: contest_id < 100000)
        String memberTotalSql = """
            SELECT COUNT(DISTINCT s.problem_id)
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            WHERE s.user_id = :userId
              AND p.contest_id < 100000
            """;
        Integer memberTotal = jdbcTemplate.queryForObject(memberTotalSql, new MapSqlParameterSource("userId", userId), Integer.class);
        int totalMemberSolves = memberTotal != null ? memberTotal : 0;

        // 2. Member tag counts
        String memberTagsSql = """
            SELECT pt.tag, COUNT(DISTINCT s.problem_id) AS tag_count
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            JOIN cf_problem_tags pt ON p.id = pt.problem_id
            WHERE s.user_id = :userId
              AND p.contest_id < 100000
            GROUP BY pt.tag
            """;
        Map<String, Integer> memberTagCounts = new HashMap<>();
        jdbcTemplate.query(memberTagsSql, new MapSqlParameterSource("userId", userId), rs -> {
            memberTagCounts.put(rs.getString("tag"), rs.getInt("tag_count"));
        });

        // 3. Member "other" tag solves: solves having no core tags
        String memberOtherSql = """
            SELECT COUNT(DISTINCT s.problem_id)
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            WHERE s.user_id = :userId
              AND p.contest_id < 100000
              AND s.problem_id NOT IN (
                  SELECT pt.problem_id
                  FROM cf_problem_tags pt
                  WHERE pt.tag IN (:coreTags)
              )
            """;
        Integer memberOther = jdbcTemplate.queryForObject(memberOtherSql,
                new MapSqlParameterSource("userId", userId).addValue("coreTags", CORE_TAGS), Integer.class);
        int memberOtherCount = memberOther != null ? memberOther : 0;

        // 4. Club-wide total counted solves
        String clubTotalSql = """
            SELECT COUNT(DISTINCT s.id)
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            WHERE p.contest_id < 100000
            """;
        Integer clubTotal = jdbcTemplate.queryForObject(clubTotalSql, new MapSqlParameterSource(), Integer.class);
        int totalClubSolves = clubTotal != null ? clubTotal : 0;

        // 5. Club-wide tag counts
        String clubTagsSql = """
            SELECT pt.tag, COUNT(DISTINCT s.id) AS club_tag_count
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            JOIN cf_problem_tags pt ON p.id = pt.problem_id
            WHERE p.contest_id < 100000
            GROUP BY pt.tag
            """;
        Map<String, Integer> clubTagCounts = new HashMap<>();
        jdbcTemplate.query(clubTagsSql, new MapSqlParameterSource(), rs -> {
            clubTagCounts.put(rs.getString("tag"), rs.getInt("club_tag_count"));
        });

        // 6. Club-wide "other" solves
        String clubOtherSql = """
            SELECT COUNT(DISTINCT s.id)
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            WHERE p.contest_id < 100000
              AND s.problem_id NOT IN (
                  SELECT pt.problem_id
                  FROM cf_problem_tags pt
                  WHERE pt.tag IN (:coreTags)
              )
            """;
        Integer clubOther = jdbcTemplate.queryForObject(clubOtherSql,
                new MapSqlParameterSource("coreTags", CORE_TAGS), Integer.class);
        int clubOtherCount = clubOther != null ? clubOther : 0;

        List<TopicStrengthDto> topicList = new ArrayList<>();
        for (String tag : CORE_TAGS) {
            int mCount = memberTagCounts.getOrDefault(tag, 0);
            int cCount = clubTagCounts.getOrDefault(tag, 0);

            double mShare = totalMemberSolves > 0 ? (double) mCount / totalMemberSolves : 0.0;
            double cShare = totalClubSolves > 0 ? (double) cCount / totalClubSolves : 0.0;
            double gap = Math.round((mShare - cShare) * 10000.0) / 10000.0;

            topicList.add(TopicStrengthDto.builder()
                    .tag(tag)
                    .memberSolves(mCount)
                    .memberShare(Math.round(mShare * 10000.0) / 10000.0)
                    .clubSolves(cCount)
                    .clubShare(Math.round(cShare * 10000.0) / 10000.0)
                    .gap(gap)
                    .build());
        }

        // Add "other" bucket
        double otherMShare = totalMemberSolves > 0 ? (double) memberOtherCount / totalMemberSolves : 0.0;
        double otherCShare = totalClubSolves > 0 ? (double) clubOtherCount / totalClubSolves : 0.0;
        double otherGap = Math.round((otherMShare - otherCShare) * 10000.0) / 10000.0;

        topicList.add(TopicStrengthDto.builder()
                .tag("other")
                .memberSolves(memberOtherCount)
                .memberShare(Math.round(otherMShare * 10000.0) / 10000.0)
                .clubSolves(clubOtherCount)
                .clubShare(Math.round(otherCShare * 10000.0) / 10000.0)
                .gap(otherGap)
                .build());

        return TopicStrengthResponseDto.builder()
                .userId(userId)
                .codeforcesHandle(user.getCodeforcesHandle())
                .reason(null)
                .totalCountedSolves(totalMemberSolves)
                .note("A problem with multiple tags counts toward each tag")
                .topics(topicList)
                .build();
    }
}
