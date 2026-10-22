package com.cpclub.backend.codeforces.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfProblemTag;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.codeforces.repository.CfSolveRepository;
import com.cpclub.backend.stats.service.ContestHistorySyncService;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesSubmissionSyncService {

    private final CodeforcesApiClient codeforcesApiClient;
    private final CfProblemRepository cfProblemRepository;
    private final CfSolveRepository cfSolveRepository;
    private final ContestHistorySyncService contestHistorySyncService;
    private final UserRepository userRepository;
    private final SyncRunRecorder syncRunRecorder;

    /**
     * Not transactional: a first sync pages through up to a hundred rate-limited
     * requests, and a transaction around it held one of three pooled connections
     * for the whole backfill. Each write commits on its own.
     */
    public void syncMember(User user) {
        if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            return;
        }
        
        long highestIdSeen = -1;
        boolean isBackfill = user.getCfLastSubmissionId() == null;
        int count = isBackfill ? 500 : 100;
        int from = 1;
        long cursor = isBackfill ? -1 : user.getCfLastSubmissionId();
        
        Set<Integer> unratedContestIds = new HashSet<>();
        boolean continuePaging = true;
        
        while (continuePaging && from < 50000) { // Limit to 50 pages just in case
            List<CfSubmission> submissions = codeforcesApiClient.userStatus(user.getCodeforcesHandle(), from, count);
            if (submissions == null || submissions.isEmpty()) {
                break;
            }
            
            for (CfSubmission sub : submissions) {
                if (sub.getId() <= cursor) {
                    continuePaging = false;
                    break;
                }
                
                if (sub.getId() > highestIdSeen) {
                    highestIdSeen = sub.getId();
                }
                
                String verdict = sub.getVerdict();
                String pt = sub.getAuthor() != null ? sub.getAuthor().getParticipantType() : "";
                
                if ("CONTESTANT".equals(pt) || "OUT_OF_COMPETITION".equals(pt)) {
                    if (sub.getContestId() != null && sub.getContestId() < 100000) {
                        unratedContestIds.add(sub.getContestId());
                    }
                }
                
                if ("OK".equals(verdict)) {
                    saveProblemAndSolve(user, sub, pt);
                }
            }
            
            if (!continuePaging) break;
            from += count;
        }
        
        if (!unratedContestIds.isEmpty()) {
            contestHistorySyncService.syncCodeforcesUnrated(user, unratedContestIds);
        }
        
        if (highestIdSeen != -1) {
            user.setCfLastSubmissionId(Math.max(highestIdSeen, cursor));
        }
        user.setCfSyncedAt(LocalDateTime.now(ZoneOffset.UTC));
        // Only the sync columns: save(user) would write back a profile as it was
        // when this run loaded it, minutes ago.
        userRepository.updateCodeforcesSubmissionCursor(
                user.getId(), user.getCodeforcesHandle(), user.getCfLastSubmissionId(), user.getCfSyncedAt());
    }

    public void syncAll() {
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.CF_SUBMISSIONS);
        int ok = 0;
        int failed = 0;
        String errorSample = null;

        List<User> users = userRepository.findByCodeforcesHandleIsNotNull();
        for (User user : users) {
            if (user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
                continue;
            }
            try {
                syncMember(user);
                ok++;
            } catch (Exception e) {
                log.error("Failed to sync CF submissions for user {}: {}", user.getCodeforcesHandle(), e.getMessage());
                failed++;
                if (errorSample == null) {
                    errorSample = user.getCodeforcesHandle() + ": " + e.getMessage();
                }
            }
        }
        run.complete(ok, failed, errorSample);
    }

    @EventListener(condition = "#event.job.name() == 'CF_SUBMISSIONS'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        syncAll();
    }
    
    private void saveProblemAndSolve(User user, CfSubmission sub, String participantType) {
        if (sub.getProblem() == null || sub.getProblem().getIndex() == null) return;
        
        String key = sub.getProblem().getContestId() != null ? 
            sub.getProblem().getContestId() + "/" + sub.getProblem().getIndex() : 
            sub.getProblem().getProblemsetName() + "/" + sub.getProblem().getIndex();
            
        CfProblem prob = cfProblemRepository.findByProblemKey(key).orElseGet(() -> {
            CfProblem p = new CfProblem();
            p.setProblemKey(key);
            p.setContestId(sub.getProblem().getContestId());
            p.setProblemsetName(sub.getProblem().getProblemsetName());
            p.setProblemIndex(sub.getProblem().getIndex());
            p.setName(sub.getProblem().getName() != null ? sub.getProblem().getName() : key);
            p.setRating(sub.getProblem().getRating());
            p.setSolvedCount(0); // Optional
            return cfProblemRepository.save(p);
        });

        // Ensure problem rating is populated if it was previously null
        if (prob.getRating() == null && sub.getProblem().getRating() != null) {
            prob.setRating(sub.getProblem().getRating());
            cfProblemRepository.save(prob);
        }
        
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochSecond(sub.getCreationTimeSeconds()), ZoneOffset.UTC);
        
        CfSolve solve = cfSolveRepository.findByUserAndProblemId(user, prob.getId()).orElse(null);
        if (solve == null) {
            solve = CfSolve.builder()
                .user(user)
                .problem(prob)
                .firstAcAt(time)
                .firstAcSubmissionId(sub.getId())
                .participantType(participantType)
                .build();
            cfSolveRepository.save(solve);
        } else {
            if (time.isBefore(solve.getFirstAcAt())) {
                solve.setFirstAcAt(time);
                solve.setFirstAcSubmissionId(sub.getId());
                solve.setParticipantType(participantType);
                cfSolveRepository.save(solve);
            }
        }
    }
}
