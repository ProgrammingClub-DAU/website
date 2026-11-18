package com.cpclub.backend.codeforces.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.stats.service.ContestHistorySyncService;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesSubmissionSyncService {

    private final CodeforcesApiClient codeforcesApiClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final TransactionTemplate transaction;
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
            
            List<CfSubmission> accepted = new ArrayList<>();
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
                    accepted.add(sub);
                }
            }
            saveSolves(user, accepted);
            
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
    
    /** New problems are added; a stored problem only gains a rating it lacked. */
    private static final String UPSERT_PROBLEM = """
            INSERT INTO cf_problems (problem_key, contest_id, problemset_name, problem_index, name, rating, solved_count)
            VALUES (:key, :contestId, :problemsetName, :problemIndex, :name, :rating, 0)
            ON CONFLICT (problem_key) DO UPDATE SET rating = EXCLUDED.rating
            WHERE cf_problems.rating IS NULL AND EXCLUDED.rating IS NOT NULL
            """;

    /** A new solve is added; a stored one is replaced only by an earlier accepted submission. */
    private static final String UPSERT_SOLVE = """
            INSERT INTO cf_solves (user_id, problem_id, first_ac_at, first_ac_submission_id, participant_type)
            VALUES (:userId, :problemId, :firstAcAt, :submissionId, :participantType)
            ON CONFLICT (user_id, problem_id) DO UPDATE SET
                first_ac_at = EXCLUDED.first_ac_at,
                first_ac_submission_id = EXCLUDED.first_ac_submission_id,
                participant_type = EXCLUDED.participant_type
            WHERE EXCLUDED.first_ac_at < cf_solves.first_ac_at
            """;

    /**
     * Records one page of accepted submissions in three statements, two of them
     * batched, in one transaction. This used to take two to four round trips to
     * the database per accepted submission, each committed on its own, so a new
     * member's first sync -- often a thousand or more solves -- took minutes,
     * and longer still with the database in another data centre.
     */
    private void saveSolves(User user, List<CfSubmission> accepted) {
        // Each problem's earliest accepted submission on this page. Codeforces
        // lists newest first; on a tie the first seen is kept, as before.
        Map<String, CfSubmission> earliest = new LinkedHashMap<>();
        for (CfSubmission sub : accepted) {
            String key = problemKey(sub);
            if (key != null) {
                earliest.merge(key, sub, (kept, other) ->
                        other.getCreationTimeSeconds() < kept.getCreationTimeSeconds() ? other : kept);
            }
        }
        if (earliest.isEmpty()) return;

        transaction.executeWithoutResult(status -> {
            List<MapSqlParameterSource> problems = new ArrayList<>();
            earliest.forEach((key, sub) -> problems.add(new MapSqlParameterSource()
                    .addValue("key", key)
                    .addValue("contestId", sub.getProblem().getContestId())
                    .addValue("problemsetName", truncate(sub.getProblem().getProblemsetName(), 50))
                    .addValue("problemIndex", truncate(sub.getProblem().getIndex(), 10))
                    .addValue("name", truncate(sub.getProblem().getName() != null ? sub.getProblem().getName() : key, 255))
                    .addValue("rating", sub.getProblem().getRating())));
            jdbc.batchUpdate(UPSERT_PROBLEM, problems.toArray(new MapSqlParameterSource[0]));

            Map<String, Long> problemIds = new HashMap<>();
            jdbc.query("SELECT id, problem_key FROM cf_problems WHERE problem_key IN (:keys)",
                    new MapSqlParameterSource("keys", earliest.keySet()),
                    rs -> { problemIds.put(rs.getString("problem_key"), rs.getLong("id")); });

            List<MapSqlParameterSource> solves = new ArrayList<>();
            earliest.forEach((key, sub) -> solves.add(new MapSqlParameterSource()
                    .addValue("userId", user.getId())
                    .addValue("problemId", problemIds.get(key))
                    .addValue("firstAcAt", LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(sub.getCreationTimeSeconds()), ZoneOffset.UTC))
                    .addValue("submissionId", sub.getId())
                    .addValue("participantType",
                            sub.getAuthor() != null ? sub.getAuthor().getParticipantType() : "")));
            jdbc.batchUpdate(UPSERT_SOLVE, solves.toArray(new MapSqlParameterSource[0]));
        });
    }

    /** "contestId/index", or "problemsetName/index" for problems outside a contest. Null when unusable. */
    private static String problemKey(CfSubmission sub) {
        if (sub.getProblem() == null || sub.getProblem().getIndex() == null) return null;
        return sub.getProblem().getContestId() != null
                ? sub.getProblem().getContestId() + "/" + sub.getProblem().getIndex()
                : sub.getProblem().getProblemsetName() + "/" + sub.getProblem().getIndex();
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
