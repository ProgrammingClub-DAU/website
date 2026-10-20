package com.cpclub.backend.stats.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.stats.repository.ContestParticipationRepository;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private final CodeforcesApiClient codeforcesApiClient;
    private final ContestParticipationRepository contestParticipationRepository;
    private final UserRepository userRepository;
    private final SyncRunRecorder syncRunRecorder;
    private final RateLimiter leetcodeRateLimiter;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ContestHistorySyncService(
            CodeforcesApiClient codeforcesApiClient,
            ContestParticipationRepository contestParticipationRepository,
            UserRepository userRepository,
            SyncRunRecorder syncRunRecorder,
            @Qualifier("leetcodeRateLimiter") RateLimiter leetcodeRateLimiter
    ) {
        this.codeforcesApiClient = codeforcesApiClient;
        this.contestParticipationRepository = contestParticipationRepository;
        this.userRepository = userRepository;
        this.syncRunRecorder = syncRunRecorder;
        this.leetcodeRateLimiter = leetcodeRateLimiter;
    }

    private Map<Integer, JsonNode> codeforcesContestCache = new HashMap<>();

    public void refreshCodeforcesContestCache() {
        try {
            List<JsonNode> contests = codeforcesApiClient.contestList(false);
            Map<Integer, JsonNode> newCache = new HashMap<>();
            for (JsonNode c : contests) {
                if (c.has("id")) {
                    newCache.put(c.get("id").asInt(), c);
                }
            }
            this.codeforcesContestCache = newCache;
        } catch (Exception e) {
            log.error("Failed to refresh CF contest cache", e);
        }
    }

    @Transactional
    public void syncCodeforcesRated(User user, List<JsonNode> ratingChanges) {
        if (ratingChanges == null) return;
        
        for (JsonNode rc : ratingChanges) {
            int contestId = rc.get("contestId").asInt();
            String externalContestId = String.valueOf(contestId);
            
            ContestParticipation participation = contestParticipationRepository
                .findByUserAndPlatformAndExternalContestId(user, Platform.CODEFORCES, externalContestId)
                .orElseGet(() -> ContestParticipation.builder()
                    .user(user)
                    .platform(Platform.CODEFORCES)
                    .externalContestId(externalContestId)
                    .build());
            
            participation.setRated(true);
            participation.setContestName(rc.has("contestName") ? rc.get("contestName").asText() : "Contest " + contestId);
            participation.setContestRank(rc.has("rank") ? rc.get("rank").asInt() : null);
            participation.setOldRating(rc.has("oldRating") ? rc.get("oldRating").asInt() : null);
            participation.setNewRating(rc.has("newRating") ? rc.get("newRating").asInt() : null);
            
            long startTimeSeconds = 0;
            JsonNode cached = codeforcesContestCache.get(contestId);
            if (cached != null && cached.has("startTimeSeconds")) {
                startTimeSeconds = cached.get("startTimeSeconds").asLong();
            } else if (rc.has("ratingUpdateTimeSeconds")) {
                startTimeSeconds = rc.get("ratingUpdateTimeSeconds").asLong();
            }
            participation.setStartedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimeSeconds), ZoneOffset.UTC));
            
            contestParticipationRepository.save(participation);
        }
    }

    @Transactional
    public void syncCodeforcesUnrated(User user, Set<Integer> contestIds) {
        for (Integer cid : contestIds) {
            String externalContestId = String.valueOf(cid);
            if (contestParticipationRepository.findByUserAndPlatformAndExternalContestId(user, Platform.CODEFORCES, externalContestId).isEmpty()) {
                long startTimeSeconds = 0;
                String contestName = "Contest " + cid;
                JsonNode cached = codeforcesContestCache.get(cid);
                if (cached != null) {
                    if (cached.has("startTimeSeconds")) startTimeSeconds = cached.get("startTimeSeconds").asLong();
                    if (cached.has("name")) contestName = cached.get("name").asText();
                }
                
                ContestParticipation p = ContestParticipation.builder()
                    .user(user)
                    .platform(Platform.CODEFORCES)
                    .externalContestId(externalContestId)
                    .contestName(contestName)
                    .startedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimeSeconds), ZoneOffset.UTC))
                    .rated(false)
                    .build();
                contestParticipationRepository.save(p);
            }
        }
    }

    @Transactional
    public void syncUserContests(User user) {
        if (user.getCodeforcesHandle() != null && !user.getCodeforcesHandle().isBlank()) {
            try {
                refreshCodeforcesContestCache();
                List<JsonNode> ratingChanges = codeforcesApiClient.userRating(user.getCodeforcesHandle());
                syncCodeforcesRated(user, ratingChanges);
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

    @Transactional
    public void syncLeetCodeContests(User user) {
        leetcodeRateLimiter.acquire();
        String handle = user.getLeetcodeHandle();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.REFERER, "https://leetcode.com");
            headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            Map<String, Object> body = Map.of(
                    "query", LC_CONTEST_HISTORY_QUERY,
                    "variables", Map.of("username", handle)
            );

            String jsonString = restTemplate.postForObject(
                    LEETCODE_GRAPHQL_URL,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (jsonString == null || jsonString.isBlank()) {
                return;
            }

            JsonNode response = OBJECT_MAPPER.readTree(jsonString);
            if (response == null || !response.has("data") || !response.get("data").has("userContestRankingHistory")) {
                return;
            }

            JsonNode history = response.get("data").get("userContestRankingHistory");
            if (!history.isArray()) return;

            Integer prevRating = null;
            for (JsonNode entry : history) {
                if (!entry.has("attended") || !entry.get("attended").asBoolean()) {
                    continue;
                }

                JsonNode contest = entry.get("contest");
                if (contest == null || !contest.has("title")) continue;

                String contestTitle = contest.get("title").asText();
                String extId = contestTitle.toLowerCase().replaceAll("[^a-z0-9]+", "-");
                Integer newRating = entry.has("rating") && !entry.get("rating").isNull()
                        ? (int) Math.round(entry.get("rating").asDouble())
                        : null;
                if (newRating == null) continue;
                Integer rank = entry.has("ranking") && !entry.get("ranking").isNull() ? entry.get("ranking").asInt() : null;
                long startTime = contest.has("startTime") ? contest.get("startTime").asLong() : 0;

                ContestParticipation participation = contestParticipationRepository
                        .findByUserAndPlatformAndExternalContestId(user, Platform.LEETCODE, extId)
                        .orElseGet(() -> ContestParticipation.builder()
                                .user(user)
                                .platform(Platform.LEETCODE)
                                .externalContestId(extId)
                                .build());

                participation.setRated(true);
                participation.setContestName(contestTitle);
                participation.setContestRank(rank);
                participation.setOldRating(prevRating);
                participation.setNewRating(newRating);
                participation.setStartedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneOffset.UTC));

                contestParticipationRepository.save(participation);
                prevRating = newRating;
            }
        } catch (Exception e) {
            log.error("Failed to query LeetCode contest ranking history for {}: {}", handle, e.getMessage());
        }
    }

    @Transactional
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
                List<JsonNode> ratingChanges = codeforcesApiClient.userRating(user.getCodeforcesHandle());
                syncCodeforcesRated(user, ratingChanges);
                ok++;
            } catch (Exception e) {
                log.error("Failed to sync CF contests for user {}", user.getCodeforcesHandle(), e);
                failed++;
                if (errorSample == null) errorSample = user.getCodeforcesHandle() + ": " + e.getMessage();
            }
        }
        run.complete(ok, failed, errorSample);
    }

    @Transactional
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
                log.error("Failed to sync LC contests for user {}", user.getLeetcodeHandle(), e);
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
}
