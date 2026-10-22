package com.cpclub.backend.stats.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfContest;
import com.cpclub.backend.codeforces.dto.CfRatingChange;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.stats.repository.ContestParticipationRepository;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Keeps contest_participations in step with each member's Codeforces and
 * LeetCode contest history.
 *
 * <p>Every timestamp is UTC (D31). None of these methods is transactional: each
 * save commits on its own, so one member's bad data cannot roll back another's,
 * and no connection is held across a rate-limited HTTP call.</p>
 */
@Service
@Slf4j
public class ContestHistorySyncService {

    private static final String LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql";
    private static final String LC_CONTEST_HISTORY_QUERY = """
            query userContestRankingHistory($username: String!) {
              userContestRankingHistory(username: $username) {
                attended
                rating
                ranking
                contest {
                  title
                  startTime
                }
              }
            }
            """;

    /** What a row gets when its contest is not in the cache. Healed on a later sync. */
    private static final LocalDateTime UNKNOWN_START = LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
    private static final Duration CONTEST_CACHE_TTL = Duration.ofHours(12);

    private final CodeforcesApiClient codeforcesApiClient;
    private final ContestParticipationRepository contestParticipationRepository;
    private final UserRepository userRepository;
    private final SyncRunRecorder syncRunRecorder;
    private final RateLimiter leetcodeRateLimiter;
    private final RestTemplate restTemplate;
    private final JsonMapper json = JsonMapper.builder().build();

    public ContestHistorySyncService(
            CodeforcesApiClient codeforcesApiClient,
            ContestParticipationRepository contestParticipationRepository,
            UserRepository userRepository,
            SyncRunRecorder syncRunRecorder,
            @Qualifier("leetcodeRateLimiter") RateLimiter leetcodeRateLimiter,
            RestTemplate restTemplate
    ) {
        this.codeforcesApiClient = codeforcesApiClient;
        this.contestParticipationRepository = contestParticipationRepository;
        this.userRepository = userRepository;
        this.syncRunRecorder = syncRunRecorder;
        this.leetcodeRateLimiter = leetcodeRateLimiter;
        this.restTemplate = restTemplate;
    }

    /** Replaced wholesale on refresh, so readers on other threads never see a half-built map. */
    private volatile Map<Integer, CfContest> codeforcesContestCache = Map.of();
    private volatile Instant lastCacheRefresh = null;

    public synchronized void refreshCodeforcesContestCache() {
        if (!codeforcesContestCache.isEmpty() && lastCacheRefresh != null
                && Instant.now().minus(CONTEST_CACHE_TTL).isBefore(lastCacheRefresh)) {
            return;
        }
        try {
            Map<Integer, CfContest> fresh = new HashMap<>();
            for (CfContest c : codeforcesApiClient.contestList(false)) {
                if (c.getId() != null) {
                    fresh.put(c.getId(), c);
                }
            }
            this.codeforcesContestCache = fresh;
            this.lastCacheRefresh = Instant.now();
        } catch (Exception e) {
            log.error("Failed to refresh CF contest cache: {}", e.getMessage());
        }
    }

    public void syncCodeforcesRated(User user, List<CfRatingChange> ratingChanges) {
        if (ratingChanges == null || ratingChanges.isEmpty()) return;

        Map<String, ContestParticipation> existing = existingByContestId(user, Platform.CODEFORCES);
        for (CfRatingChange rc : ratingChanges) {
            if (rc.getContestId() == null) continue;
            int contestId = rc.getContestId();
            String externalContestId = String.valueOf(contestId);

            ContestParticipation participation = existing.getOrDefault(externalContestId,
                    ContestParticipation.builder()
                            .user(user)
                            .platform(Platform.CODEFORCES)
                            .externalContestId(externalContestId)
                            .build());

            participation.setRated(true);
            participation.setContestName(rc.getContestName() != null ? rc.getContestName() : "Contest " + contestId);
            participation.setContestRank(rc.getRank());
            participation.setOldRating(rc.getOldRating());
            participation.setNewRating(rc.getNewRating());

            CfContest cached = codeforcesContestCache.get(contestId);
            if (cached != null && cached.getStartTimeSeconds() != null) {
                participation.setStartedAt(fromEpoch(cached.getStartTimeSeconds()));
            } else if (rc.getRatingUpdateTimeSeconds() != null) {
                participation.setStartedAt(fromEpoch(rc.getRatingUpdateTimeSeconds()));
            } else if (participation.getStartedAt() == null) {
                participation.setStartedAt(UNKNOWN_START);
            }

            contestParticipationRepository.save(participation);
        }
    }

    /**
     * Records the contests a member entered without a rating change.
     *
     * <p>Fills the contest cache first. It used to read whatever happened to be
     * cached, and the six-hourly pipeline calls this (via the submission sync)
     * before anything has filled it -- so after every restart these rows were
     * saved as "Contest 1234" starting on 1 January 1970, and were invisible to
     * every week, month and year board. Rows already saved that way are repaired
     * here when the contest is found.</p>
     */
    public void syncCodeforcesUnrated(User user, Set<Integer> contestIds) {
        if (contestIds == null || contestIds.isEmpty()) return;
        refreshCodeforcesContestCache();

        Map<String, ContestParticipation> existing = existingByContestId(user, Platform.CODEFORCES);
        for (Integer cid : contestIds) {
            String externalContestId = String.valueOf(cid);
            CfContest cached = codeforcesContestCache.get(cid);
            ContestParticipation row = existing.get(externalContestId);

            if (row == null) {
                contestParticipationRepository.save(ContestParticipation.builder()
                        .user(user)
                        .platform(Platform.CODEFORCES)
                        .externalContestId(externalContestId)
                        .contestName(cached != null && cached.getName() != null ? cached.getName() : "Contest " + cid)
                        .startedAt(cached != null && cached.getStartTimeSeconds() != null
                                ? fromEpoch(cached.getStartTimeSeconds()) : UNKNOWN_START)
                        .rated(false)
                        .build());
            } else if (cached != null && needsRepair(row)) {
                if (cached.getStartTimeSeconds() != null) row.setStartedAt(fromEpoch(cached.getStartTimeSeconds()));
                if (cached.getName() != null) row.setContestName(cached.getName());
                contestParticipationRepository.save(row);
            }
        }
    }

    private static boolean needsRepair(ContestParticipation row) {
        return row.getStartedAt() == null
                || !row.getStartedAt().isAfter(UNKNOWN_START)
                || row.getContestName() == null
                || row.getContestName().startsWith("Contest ");
    }

    /** Refreshes one member's history on both platforms. Never throws. */
    public void syncUserContests(User user) {
        if (user.getCodeforcesHandle() != null && !user.getCodeforcesHandle().isBlank()) {
            try {
                refreshCodeforcesContestCache();
                syncCodeforcesRated(user, codeforcesApiClient.userRating(user.getCodeforcesHandle()));
            } catch (Exception e) {
                log.error("Failed to sync CF contest history for {}: {}", user.getCodeforcesHandle(), e.getMessage());
            }
        }
        if (user.getLeetcodeHandle() != null && !user.getLeetcodeHandle().isBlank()) {
            try {
                syncLeetCodeContests(user);
            } catch (Exception e) {
                log.error("Failed to sync LC contest history for {}: {}", user.getLeetcodeHandle(), e.getMessage());
            }
        }
    }

    /**
     * Refreshes one member's LeetCode contest history.
     *
     * @throws IllegalStateException when LeetCode does not answer usefully. This used
     *         to be logged and swallowed, so LC_CONTESTS reported every member as a
     *         success even while LeetCode was refusing every request.
     */
    public void syncLeetCodeContests(User user) {
        leetcodeRateLimiter.acquire();
        String handle = user.getLeetcodeHandle();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.REFERER, "https://leetcode.com");
        headers.set(HttpHeaders.USER_AGENT, LeetCodeSyncService.BROWSER_USER_AGENT);

        Map<String, Object> body = Map.of(
                "query", LC_CONTEST_HISTORY_QUERY,
                "variables", Map.of("username", handle)
        );

        String jsonString = restTemplate.postForObject(LEETCODE_GRAPHQL_URL, new HttpEntity<>(body, headers), String.class);
        if (jsonString == null || jsonString.isBlank()) {
            throw new IllegalStateException("empty response from LeetCode");
        }

        JsonNode response = json.readTree(jsonString);
        JsonNode data = response.path("data");
        if (data.isMissingNode() || data.isNull()) {
            throw new IllegalStateException("LeetCode returned no data" + firstGraphQlError(response));
        }

        JsonNode history = data.path("userContestRankingHistory");
        // Null history is LeetCode's answer for a valid user who has never contested.
        if (!history.isArray()) return;

        Map<String, ContestParticipation> existing = existingByContestId(user, Platform.LEETCODE);
        Integer prevRating = null;
        for (JsonNode entry : history) {
            if (!entry.path("attended").asBoolean(false)) {
                continue;
            }

            JsonNode contest = entry.path("contest");
            String contestTitle = contest.path("title").asString("");
            if (contestTitle.isBlank()) continue;

            JsonNode ratingNode = entry.path("rating");
            if (ratingNode.isMissingNode() || ratingNode.isNull()) continue;
            int newRating = (int) Math.round(ratingNode.asDouble());

            JsonNode rankingNode = entry.path("ranking");
            Integer rank = rankingNode.isMissingNode() || rankingNode.isNull() ? null : rankingNode.asInt(0);
            long startTime = contest.path("startTime").asLong(0L);
            String extId = contestTitle.toLowerCase().replaceAll("[^a-z0-9]+", "-");

            ContestParticipation participation = existing.getOrDefault(extId,
                    ContestParticipation.builder()
                            .user(user)
                            .platform(Platform.LEETCODE)
                            .externalContestId(extId)
                            .build());

            participation.setRated(true);
            participation.setContestName(contestTitle);
            participation.setContestRank(rank);
            participation.setOldRating(prevRating);
            participation.setNewRating(newRating);
            participation.setStartedAt(fromEpoch(startTime));

            contestParticipationRepository.save(participation);
            prevRating = newRating;
        }
    }

    private static String firstGraphQlError(JsonNode response) {
        JsonNode errors = response.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            return ": " + errors.get(0).path("message").asString("");
        }
        return "";
    }

    public void syncAllCodeforcesContests() {
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.CF_CONTESTS);
        int ok = 0;
        int failed = 0;
        String errorSample = null;

        refreshCodeforcesContestCache();
        List<User> users = userRepository.findByCodeforcesHandleIsNotNull();
        for (User user : users) {
            if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) continue;
            try {
                syncCodeforcesRated(user, codeforcesApiClient.userRating(user.getCodeforcesHandle()));
                ok++;
            } catch (Exception e) {
                log.warn("Failed to sync CF contests for user {}: {}", user.getCodeforcesHandle(), e.getMessage());
                failed++;
                if (errorSample == null) errorSample = user.getCodeforcesHandle() + ": " + e.getMessage();
            }
        }
        run.complete(ok, failed, errorSample);
    }

    public void syncAllLeetCodeContests() {
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.LC_CONTESTS);
        int ok = 0;
        int failed = 0;
        String errorSample = null;

        List<User> users = userRepository.findByLeetcodeHandleIsNotNull();
        for (User user : users) {
            if (user.getLeetcodeHandle() == null || user.getLeetcodeHandle().isBlank()) continue;
            try {
                syncLeetCodeContests(user);
                ok++;
            } catch (Exception e) {
                log.warn("Failed to sync LC contests for user {}: {}", user.getLeetcodeHandle(), e.getMessage());
                failed++;
                if (errorSample == null) errorSample = user.getLeetcodeHandle() + ": " + e.getMessage();
            }
        }
        run.complete(ok, failed, errorSample);
    }

    @EventListener(condition = "#event.job.name() == 'CF_CONTESTS'")
    public void handleManualCfContestsTrigger(SyncTriggerEvent event) {
        syncAllCodeforcesContests();
    }

    @EventListener(condition = "#event.job.name() == 'LC_CONTESTS'")
    public void handleManualLcContestsTrigger(SyncTriggerEvent event) {
        syncAllLeetCodeContests();
    }

    /** One query per member instead of one per contest. */
    private Map<String, ContestParticipation> existingByContestId(User user, Platform platform) {
        return contestParticipationRepository.findByUserAndPlatform(user, platform).stream()
                .collect(Collectors.toMap(ContestParticipation::getExternalContestId, Function.identity(), (a, b) -> a));
    }

    private static LocalDateTime fromEpoch(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }
}
