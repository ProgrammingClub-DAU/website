package com.cpclub.backend.codeforces.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.client.CodeforcesApiClient.ProblemsetResult;
import com.cpclub.backend.codeforces.dto.CfProblem;
import com.cpclub.backend.codeforces.entity.CfProblemTag;
import com.cpclub.backend.codeforces.entity.CfProblemTagId;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeforcesProblemsetSyncService {

    private final CodeforcesApiClient apiClient;
    private final CfProblemRepository cfProblemRepository;
    private final SyncRunRecorder syncRunRecorder;

    @org.springframework.context.event.EventListener(condition = "#event.job.name() == 'CF_PROBLEMSET'")
    public void handleManualTrigger(com.cpclub.backend.sync.event.SyncTriggerEvent event) {
        syncProblemset();
    }

    @Scheduled(cron = "0 0 3 * * SUN", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    @Transactional
    public void syncProblemset() {
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.CF_PROBLEMSET);
        try {
            ProblemsetResult result = apiClient.problemsetProblems();
            List<CfProblem> problems = result.getProblems();
            List<java.util.Map<String, Object>> stats = (List<java.util.Map<String, Object>>) result.getProblemStatistics();
            
            Map<String, Integer> solveCounts = stats.stream()
                .collect(Collectors.toMap(
                    node -> {
                        Object contestId = node.get("contestId");
                        String index = String.valueOf(node.get("index"));
                        return contestId != null ? contestId.toString() + "/" + index : "null/" + index;
                    },
                    node -> node.containsKey("solvedCount") ? ((Number) node.get("solvedCount")).intValue() : 0,
                    (v1, v2) -> v1 // In case of duplicate keys
                ));

            for (CfProblem dto : problems) {
                String problemKey = (dto.getContestId() != null ? dto.getContestId() : "null") + "/" + dto.getIndex();
                
                com.cpclub.backend.codeforces.entity.CfProblem entity = cfProblemRepository.findByProblemKey(problemKey)
                    .orElseGet(() -> com.cpclub.backend.codeforces.entity.CfProblem.builder()
                        .problemKey(problemKey)
                        .contestId(dto.getContestId())
                        .problemIndex(dto.getIndex())
                        .name(dto.getName())
                        .build());

                entity.setRating(dto.getRating());
                entity.setSolvedCount(solveCounts.getOrDefault(problemKey, 0));
                
                if (entity.getTags() == null) {
                    entity.setTags(new HashSet<>());
                } else {
                    entity.getTags().clear();
                }
                
                if (dto.getTags() != null) {
                    for (String tagName : dto.getTags()) {
                        CfProblemTagId tagId = new CfProblemTagId(entity.getId(), tagName);
                        CfProblemTag tag = new CfProblemTag(tagId, entity, tagName);
                        entity.getTags().add(tag);
                    }
                }
                
                cfProblemRepository.save(entity);
            }
            
            // Assume we can say OK for all problems overall
            run.ok();
            run.finish();
        } catch (Exception e) {
            log.error("Problemset sync failed", e);
            run.failed(e.getMessage());
            run.finish();
        }
    }
}
