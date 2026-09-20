package com.cpclub.backend.stats.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.stats.repository.ContestParticipationRepository;
import com.cpclub.backend.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContestHistorySyncService {

    private final CodeforcesApiClient codeforcesApiClient;
    private final ContestParticipationRepository contestParticipationRepository;

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
}
