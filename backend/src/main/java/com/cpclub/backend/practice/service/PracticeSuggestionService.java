package com.cpclub.backend.practice.service;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.practice.dto.PracticeSuggestionDto;
import com.cpclub.backend.practice.dto.PracticeSuggestionsResponseDto;
import com.cpclub.backend.practice.dto.TryAgainDto;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A member's daily practice: problems picked by their estimated skill, and the
 * problems they tried and have not solved yet (Section 1.4, 6.5).
 *
 * <p>Picking: {@link SkillProfileService} estimates the member's level overall
 * and per topic. Problems are drawn from the member's focus topics at a rating
 * where the estimated chance of solving them is {@link #MIN_CHANCE}-{@link #MAX_CHANCE}
 * -- hard enough to learn from, likely enough to finish -- judged against the
 * member's level in that topic, not their overall rating. Each topic offers its
 * {@link #POOL_PER_TOPIC} most-solved such problems (well-known, well-tested), and
 * the day's {@link #SUGGESTIONS} are a shuffle of those, seeded by member and
 * date: two members with the same profile get different lists, and a member gets
 * a new mix each day. Short of focus problems, the rest come from all topics at
 * the overall level.</p>
 *
 * <p>Cached per member per IST date in memory (D30). A problem solved since is
 * left out when the cached list is served again.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PracticeSuggestionService {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    static final int SUGGESTIONS = 10;
    static final int TRY_AGAIN = 6;
    static final double MIN_CHANCE = 0.40;
    static final double MAX_CHANCE = 0.70;
    static final int POOL_PER_TOPIC = 15;
    /** The most-solved problems in the rating range considered, before the chance filter. */
    private static final int CANDIDATE_LIMIT = 400;

    private final SkillProfileService skillProfileService;
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
            PracticeSuggestionsResponseDto current = withoutSolved(userId, cached);
            if (current != cached) cache.put(cacheKey, current);
            return current;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            PracticeSuggestionsResponseDto response = PracticeSuggestionsResponseDto.builder()
                    .weakTags(List.of())
                    .reason("NO_CODEFORCES_HANDLE")
                    .suggestions(List.of())
                    .tryAgain(List.of())
                    .build();
            cache.put(cacheKey, response);
            return response;
        }

        SkillProfile profile = skillProfileService.profileOf(user);
        List<TryAgainDto> tryAgain = tryAgain(userId, profile);
        Set<Long> exclude = new HashSet<>();
        tryAgain.forEach(t -> exclude.add(t.getProblemId()));
        List<PracticeSuggestionDto> suggestions =
                pick(userId, profile, new Random(Objects.hash(userId, todayIst)), exclude);

        PracticeSuggestionsResponseDto response = PracticeSuggestionsResponseDto.builder()
                .ratingBandMin(suggestions.stream().map(PracticeSuggestionDto::getRating).min(Integer::compare).orElse(null))
                .ratingBandMax(suggestions.stream().map(PracticeSuggestionDto::getRating).max(Integer::compare).orElse(null))
                .weakTags(profile.weakTags())
                .overallLevel(profile.overallLevel())
                .suggestions(suggestions)
                .tryAgain(tryAgain)
                .build();
        // Earlier days' lists are never read again; without this they piled up until a restart.
        String today = ":" + todayIst;
        cache.keySet().removeIf(key -> !key.endsWith(today));
        cache.put(cacheKey, response);
        return response;
    }

    /** The day's picks: focus topics first, topped up from all topics at the overall level. */
    private List<PracticeSuggestionDto> pick(Long userId, SkillProfile profile, Random random, Set<Long> exclude) {
        List<PracticeSuggestionDto> picked = new ArrayList<>();
        List<String> focus = profile.weakTags();

        if (!focus.isEmpty()) {
            int lowest = focus.stream().mapToInt(profile::levelFor).min().orElseThrow();
            int highest = focus.stream().mapToInt(profile::levelFor).max().orElseThrow();
            Map<String, List<PracticeSuggestionDto>> pools = new LinkedHashMap<>();
            focus.forEach(tag -> pools.put(tag, new ArrayList<>()));

            for (PracticeSuggestionDto candidate : candidates(userId, lowest, highest, focus, exclude)) {
                // A problem in two focus topics builds the weaker one.
                String topic = candidate.getTags().stream()
                        .filter(pools::containsKey)
                        .min(Comparator.comparingInt(profile::levelFor))
                        .orElse(null);
                if (topic == null || pools.get(topic).size() >= POOL_PER_TOPIC) continue;
                if (assess(candidate, topic, profile.levelFor(topic))) pools.get(topic).add(candidate);
            }
            pools.values().forEach(pool -> Collections.shuffle(pool, random));
            // Round robin, weakest topic first, so every focus topic is in the list.
            for (int round = 0; picked.size() < SUGGESTIONS && round < POOL_PER_TOPIC; round++) {
                for (List<PracticeSuggestionDto> pool : pools.values()) {
                    if (round < pool.size() && picked.size() < SUGGESTIONS) picked.add(pool.get(round));
                }
            }
        }

        if (picked.size() < SUGGESTIONS) {
            int level = profile.overallLevel();
            Set<Long> taken = new HashSet<>(exclude);
            picked.forEach(p -> taken.add(p.getProblemId()));
            List<PracticeSuggestionDto> pool = new ArrayList<>();
            for (PracticeSuggestionDto candidate : candidates(userId, level, level, List.of(), taken)) {
                if (pool.size() >= POOL_PER_TOPIC * 2) break;
                if (assess(candidate, null, level)) pool.add(candidate);
            }
            Collections.shuffle(pool, random);
            pool.stream().limit(SUGGESTIONS - picked.size()).forEach(picked::add);
        }
        return picked;
    }

    /** Sets the focus and chance on a candidate; true when that chance is in the target range. */
    private static boolean assess(PracticeSuggestionDto candidate, String topic, int level) {
        double chance = SkillProfile.chance(candidate.getRating(), level);
        if (chance < MIN_CHANCE - 1e-9 || chance > MAX_CHANCE + 1e-9) return false;
        candidate.setFocusTag(topic);
        candidate.setFocusLevel(level);
        candidate.setChance((int) Math.round(chance * 100));
        return true;
    }

    /**
     * Unsolved, unskipped, rated problems from regular contests, most solved first,
     * in the rating range that the levels' target chances span, each with its tags.
     */
    private List<PracticeSuggestionDto> candidates(Long userId, int lowestLevel, int highestLevel,
                                                   List<String> tags, Set<Long> exclude) {
        StringBuilder sql = new StringBuilder("""
            SELECT p.id, p.problem_key, p.contest_id, p.problem_index, p.name, p.rating, p.solved_count
            FROM cf_problems p
            WHERE p.rating BETWEEN :minRating AND :maxRating
              AND p.contest_id IS NOT NULL AND p.contest_id < 100000
              AND p.id NOT IN (SELECT s.problem_id FROM cf_solves s WHERE s.user_id = :userId)
              AND p.id NOT IN (SELECT sk.problem_id FROM practice_suggestion_skips sk WHERE sk.user_id = :userId)
              AND p.id NOT IN (SELECT t.problem_id FROM cf_problem_tags t WHERE t.tag = '*special')
            """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("minRating", SkillProfile.ratingWithChance(lowestLevel, MAX_CHANCE))
                .addValue("maxRating", SkillProfile.ratingWithChance(highestLevel, MIN_CHANCE));
        if (!tags.isEmpty()) {
            sql.append(" AND p.id IN (SELECT pt.problem_id FROM cf_problem_tags pt WHERE pt.tag IN (:tags))");
            params.addValue("tags", tags);
        }
        sql.append(" ORDER BY p.solved_count DESC NULLS LAST, p.problem_key ASC LIMIT ").append(CANDIDATE_LIMIT);

        List<PracticeSuggestionDto> rows = jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            Integer contestId = (Integer) rs.getObject("contest_id");
            String problemIndex = rs.getString("problem_index");
            return PracticeSuggestionDto.builder()
                    .problemId(rs.getLong("id"))
                    .problemKey(rs.getString("problem_key"))
                    .contestId(contestId)
                    .problemIndex(problemIndex)
                    .name(rs.getString("name"))
                    .rating((Integer) rs.getObject("rating"))
                    .solvedCount((Integer) rs.getObject("solved_count"))
                    .url(problemUrl(contestId, problemIndex))
                    .build();
        });
        rows.removeIf(row -> exclude.contains(row.getProblemId()));
        Map<Long, List<String>> tagsById = tagsOf(rows.stream().map(PracticeSuggestionDto::getProblemId).toList());
        rows.forEach(row -> row.setTags(tagsById.getOrDefault(row.getProblemId(), List.of())));
        return rows;
    }

    /** Problems tried and not solved, most recent first, with the chance of solving each now. */
    private List<TryAgainDto> tryAgain(Long userId, SkillProfile profile) {
        List<TryAgainDto> rows = jdbcTemplate.query("""
                SELECT p.id, p.problem_key, p.contest_id, p.problem_index, p.name, p.rating,
                       a.last_verdict, a.last_attempt_at
                FROM cf_attempts a JOIN cf_problems p ON p.id = a.problem_id
                WHERE a.user_id = :userId
                  AND p.contest_id IS NOT NULL AND p.contest_id < 100000
                  AND NOT EXISTS (SELECT 1 FROM cf_solves s WHERE s.user_id = a.user_id AND s.problem_id = a.problem_id)
                  AND NOT EXISTS (SELECT 1 FROM practice_suggestion_skips sk
                                  WHERE sk.user_id = a.user_id AND sk.problem_id = a.problem_id)
                ORDER BY a.last_attempt_at DESC, p.id DESC
                """ + " LIMIT " + TRY_AGAIN,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> {
                    Integer contestId = (Integer) rs.getObject("contest_id");
                    String problemIndex = rs.getString("problem_index");
                    return TryAgainDto.builder()
                            .problemId(rs.getLong("id"))
                            .problemKey(rs.getString("problem_key"))
                            .contestId(contestId)
                            .problemIndex(problemIndex)
                            .name(rs.getString("name"))
                            .rating((Integer) rs.getObject("rating"))
                            .url(problemUrl(contestId, problemIndex))
                            .lastVerdict(rs.getString("last_verdict"))
                            .lastAttemptAt(rs.getObject("last_attempt_at", LocalDateTime.class))
                            .build();
                });
        Map<Long, List<String>> tagsById = tagsOf(rows.stream().map(TryAgainDto::getProblemId).toList());
        for (TryAgainDto row : rows) {
            List<String> tags = tagsById.getOrDefault(row.getProblemId(), List.of());
            row.setTags(tags);
            if (row.getRating() != null) {
                // Judged by the member's weakest topic among the problem's, as the picks are.
                int level = tags.stream()
                        .filter(profile.topicLevels()::containsKey)
                        .mapToInt(profile::levelFor)
                        .min()
                        .orElse(profile.overallLevel());
                row.setChance((int) Math.round(SkillProfile.chance(row.getRating(), level) * 100));
            }
        }
        return rows;
    }

    private Map<Long, List<String>> tagsOf(Collection<Long> problemIds) {
        Map<Long, List<String>> tagsById = new HashMap<>();
        if (problemIds.isEmpty()) return tagsById;
        jdbcTemplate.query("SELECT problem_id, tag FROM cf_problem_tags WHERE problem_id IN (:ids) ORDER BY tag",
                new MapSqlParameterSource("ids", problemIds),
                rs -> { tagsById.computeIfAbsent(rs.getLong("problem_id"), k -> new ArrayList<>()).add(rs.getString("tag")); });
        return tagsById;
    }

    /** The cached response, less any problem solved since it was built; the same instance when there is none. */
    private PracticeSuggestionsResponseDto withoutSolved(Long userId, PracticeSuggestionsResponseDto cached) {
        List<Long> shown = new ArrayList<>();
        cached.getSuggestions().forEach(s -> shown.add(s.getProblemId()));
        if (cached.getTryAgain() != null) cached.getTryAgain().forEach(t -> shown.add(t.getProblemId()));
        if (shown.isEmpty()) return cached;

        Set<Long> solved = new HashSet<>(jdbcTemplate.queryForList(
                "SELECT problem_id FROM cf_solves WHERE user_id = :userId AND problem_id IN (:ids)",
                new MapSqlParameterSource("userId", userId).addValue("ids", shown), Long.class));
        if (solved.isEmpty()) return cached;

        return PracticeSuggestionsResponseDto.builder()
                .ratingBandMin(cached.getRatingBandMin())
                .ratingBandMax(cached.getRatingBandMax())
                .weakTags(cached.getWeakTags())
                .reason(cached.getReason())
                .overallLevel(cached.getOverallLevel())
                .suggestions(cached.getSuggestions().stream().filter(s -> !solved.contains(s.getProblemId())).toList())
                .tryAgain(cached.getTryAgain() == null ? List.of()
                        : cached.getTryAgain().stream().filter(t -> !solved.contains(t.getProblemId())).toList())
                .build();
    }

    private static String problemUrl(Integer contestId, String problemIndex) {
        return contestId != null ? "https://codeforces.com/problemset/problem/" + contestId + "/" + problemIndex : null;
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
