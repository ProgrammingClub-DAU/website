package com.cpclub.backend.practice.service;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.practice.dto.PracticeSuggestionDto;
import com.cpclub.backend.practice.dto.PracticeSuggestionsResponseDto;
import com.cpclub.backend.practice.dto.TopicStrengthDto;
import com.cpclub.backend.practice.dto.TopicStrengthResponseDto;
import com.cpclub.backend.score.entity.PracticeSuggestionSkip;
import com.cpclub.backend.score.entity.PracticeSuggestionSkipId;
import com.cpclub.backend.score.repository.PracticeSuggestionSkipRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for computing and caching practice suggestions (Section 1.4, 6.5).
 * Cached per member per IST date in memory (D30).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeSuggestionService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final TopicStrengthService topicStrengthService;
    private final PracticeSuggestionSkipRepository practiceSuggestionSkipRepository;
    private final UserRepository userRepository;
    private final CfProblemRepository cfProblemRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    private final Map<String, PracticeSuggestionsResponseDto> cache = new ConcurrentHashMap<>();

    public PracticeSuggestionsResponseDto getSuggestions(Long userId) {
        LocalDate todayIst = LocalDate.ofInstant(clock.instant(), IST_ZONE);
        String cacheKey = userId + ":" + todayIst;

        PracticeSuggestionsResponseDto cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            PracticeSuggestionsResponseDto response = PracticeSuggestionsResponseDto.builder()
                    .ratingBandMin(null)
                    .ratingBandMax(null)
                    .weakTags(List.of())
                    .reason("NO_CODEFORCES_HANDLE")
                    .suggestions(List.of())
                    .build();
            cache.put(cacheKey, response);
            return response;
        }

        // 1. Target rating band
        int minBand;
        int maxBand;
        if (user.getRating() == null || user.getRating() <= 0) {
            minBand = 800;
            maxBand = 1000;
        } else {
            int rounded = (user.getRating() / 100) * 100;
            minBand = Math.max(800, Math.min(3500, rounded + 100));
            maxBand = Math.max(800, Math.min(3500, rounded + 300));
        }

        // 2. Weak tags
        TopicStrengthResponseDto topicStrength = topicStrengthService.getUserTopicStrength(userId);
        List<String> weakTags = new ArrayList<>();
        if (topicStrength.getTotalCountedSolves() >= 30 && topicStrength.getTopics() != null) {
            weakTags = topicStrength.getTopics().stream()
                    .filter(t -> !t.getTag().equals("other"))
                    .sorted(Comparator.comparingDouble(TopicStrengthDto::getGap))
                    .limit(3)
                    .map(TopicStrengthDto::getTag)
                    .toList();
        }

        // 3. Candidates query
        StringBuilder sql = new StringBuilder("""
            SELECT p.id, p.problem_key, p.contest_id, p.problem_index, p.name, p.rating, p.solved_count
            FROM cf_problems p
            WHERE p.rating >= :minBand AND p.rating <= :maxBand
              AND p.contest_id < 100000
              AND p.id NOT IN (SELECT s.problem_id FROM cf_solves s WHERE s.user_id = :userId)
              AND p.id NOT IN (SELECT sk.problem_id FROM practice_suggestion_skips sk WHERE sk.user_id = :userId)
            """);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("minBand", minBand)
                .addValue("maxBand", maxBand);

        if (!weakTags.isEmpty()) {
            sql.append(" AND p.id IN (SELECT pt.problem_id FROM cf_problem_tags pt WHERE pt.tag IN (:weakTags))");
            params.addValue("weakTags", weakTags);
        }

        sql.append(" ORDER BY p.solved_count DESC NULLS LAST, p.problem_key ASC LIMIT 10");

        List<PracticeSuggestionDto> candidates = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            Long problemId = rs.getLong("id");
            Integer contestId = (Integer) rs.getObject("contest_id");
            String problemIndex = rs.getString("problem_index");
            String url = contestId != null
                    ? "https://codeforces.com/problemset/problem/" + contestId + "/" + problemIndex
                    : null;

            return PracticeSuggestionDto.builder()
                    .problemId(problemId)
                    .problemKey(rs.getString("problem_key"))
                    .contestId(contestId)
                    .problemIndex(problemIndex)
                    .name(rs.getString("name"))
                    .rating((Integer) rs.getObject("rating"))
                    .solvedCount((Integer) rs.getObject("solved_count"))
                    .tags(new ArrayList<>())
                    .url(url)
                    .build();
        });

        // 4. Populate tags for suggestions
        if (!candidates.isEmpty()) {
            List<Long> problemIds = candidates.stream().map(PracticeSuggestionDto::getProblemId).toList();
            String tagsSql = "SELECT problem_id, tag FROM cf_problem_tags WHERE problem_id IN (:problemIds)";
            Map<Long, List<String>> tagMap = new HashMap<>();
            jdbcTemplate.query(tagsSql, new MapSqlParameterSource("problemIds", problemIds), rs -> {
                Long pid = rs.getLong("problem_id");
                String tag = rs.getString("tag");
                tagMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(tag);
            });
            for (PracticeSuggestionDto dto : candidates) {
                dto.setTags(tagMap.getOrDefault(dto.getProblemId(), List.of()));
            }
        }

        PracticeSuggestionsResponseDto response = PracticeSuggestionsResponseDto.builder()
                .ratingBandMin(minBand)
                .ratingBandMax(maxBand)
                .weakTags(weakTags)
                .reason(null)
                .suggestions(candidates)
                .build();

        cache.put(cacheKey, response);
        return response;
    }

    @Transactional
    public void skip(Long userId, Long problemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        CfProblem problem = cfProblemRepository.findById(problemId)
                .orElseThrow(() -> new ResourceNotFoundException("Problem not found with id: " + problemId));

        LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("UTC"));
        PracticeSuggestionSkip skip = new PracticeSuggestionSkip(
                new PracticeSuggestionSkipId(userId, problemId),
                user,
                problem,
                nowUtc
        );
        practiceSuggestionSkipRepository.save(skip);

        // Invalidate cache for user
        LocalDate todayIst = LocalDate.ofInstant(clock.instant(), IST_ZONE);
        cache.remove(userId + ":" + todayIst);
    }

    public void clearCache() {
        cache.clear();
    }
}
