package com.cpclub.backend.codeforces.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfProblemTag;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.codeforces.repository.CfSolveRepository;
import com.cpclub.backend.stats.service.ContestHistorySyncService;
import com.cpclub.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
