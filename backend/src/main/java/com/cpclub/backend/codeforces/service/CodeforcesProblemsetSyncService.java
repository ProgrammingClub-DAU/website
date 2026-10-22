package com.cpclub.backend.codeforces.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.client.CodeforcesApiClient.ProblemsetResult;
import com.cpclub.backend.codeforces.dto.CfProblem;
import com.cpclub.backend.codeforces.dto.CfProblemStatistics;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncJobGuard;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mirrors the public Codeforces problemset -- names, ratings, tags and solve counts --
 * into cf_problems and cf_problem_tags. Practice suggestions and topic strength read it.
 *
 * <p>Written as batched JDBC rather than through JPA. The JPA version loaded all of
 * roughly ten thousand problems, and their tags, into a single persistence context
 * inside one transaction: one query and one save per problem, on a 160 MB heap. That
 * is why its weekly schedule had been switched off. This version holds only keys and
 * tag names in memory, writes in batches of {@value #BATCH}, and touches only tags
 * that actually changed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeforcesProblemsetSyncService {

    private static final int BATCH = 500;

    private final CodeforcesApiClient apiClient;
    private final NamedParameterJdbcTemplate jdbc;
    private final SyncRunRecorder syncRunRecorder;
    private final SyncJobGuard syncJobGuard;

    @EventListener(condition = "#event.job.name() == 'CF_PROBLEMSET'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        syncProblemset();
    }

    /** Weekly, Sunday 03:00 IST: new problems get ratings a few days after a round. */
    @Scheduled(cron = "0 0 3 * * SUN", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public void scheduledSync() {
        syncJobGuard.runExclusive(SyncJob.CF_PROBLEMSET, this::syncProblemset);
    }

    public void syncProblemset() {
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.CF_PROBLEMSET);
        try {
            ProblemsetResult result = apiClient.problemsetProblems();
            List<CfProblem> problems = result.getProblems() != null ? result.getProblems() : List.of();

            Map<String, Integer> solveCounts = new HashMap<>();
            if (result.getProblemStatistics() != null) {
                for (CfProblemStatistics stat : result.getProblemStatistics()) {
                    if (stat.getContestId() != null && stat.getIndex() != null && stat.getSolvedCount() != null) {
                        solveCounts.put(stat.getContestId() + "/" + stat.getIndex(), stat.getSolvedCount());
                    }
                }
            }

            // Last one wins if Codeforces ever lists a key twice.
            Map<String, CfProblem> byKey = new LinkedHashMap<>();
            for (CfProblem dto : problems) {
                String key = problemKey(dto);
                if (key != null && dto.getName() != null) {
                    byKey.put(key, dto);
                }
            }

            Map<String, Long> ids = loadIds();
            upsertProblems(byKey, solveCounts, ids);
            ids = loadIds();
            int tagChanges = syncTags(byKey, ids);

            log.info("Problemset sync wrote {} problems and {} tag changes", byKey.size(), tagChanges);
            run.complete(byKey.size(), 0, null);
        } catch (Exception e) {
            log.error("Problemset sync failed", e);
            run.complete(0, 1, e.getMessage());
        }
    }

    /** Same key as the submission sync uses, so both find the same row. */
    static String problemKey(CfProblem dto) {
        if (dto.getIndex() == null) return null;
        if (dto.getContestId() != null) return dto.getContestId() + "/" + dto.getIndex();
        if (dto.getProblemsetName() != null) return dto.getProblemsetName() + "/" + dto.getIndex();
        return null;
    }

    private Map<String, Long> loadIds() {
        Map<String, Long> ids = new HashMap<>();
        jdbc.query("SELECT id, problem_key FROM cf_problems", new MapSqlParameterSource(),
                rs -> { ids.put(rs.getString("problem_key"), rs.getLong("id")); });
        return ids;
    }

    private void upsertProblems(Map<String, CfProblem> byKey, Map<String, Integer> solveCounts, Map<String, Long> ids) {
        List<MapSqlParameterSource> inserts = new ArrayList<>();
        List<MapSqlParameterSource> updates = new ArrayList<>();

        for (Map.Entry<String, CfProblem> e : byKey.entrySet()) {
            CfProblem dto = e.getValue();
            MapSqlParameterSource row = new MapSqlParameterSource()
                    .addValue("key", e.getKey())
                    .addValue("contestId", dto.getContestId())
                    .addValue("problemsetName", truncate(dto.getProblemsetName(), 50))
                    .addValue("problemIndex", truncate(dto.getIndex(), 10))
                    .addValue("name", truncate(dto.getName(), 255))
                    .addValue("rating", dto.getRating())
                    .addValue("solvedCount", solveCounts.getOrDefault(e.getKey(), 0));
            Long id = ids.get(e.getKey());
            if (id == null) {
                inserts.add(row);
            } else {
                updates.add(row.addValue("id", id));
            }
        }

        batch("""
                INSERT INTO cf_problems (problem_key, contest_id, problemset_name, problem_index, name, rating, solved_count)
                VALUES (:key, :contestId, :problemsetName, :problemIndex, :name, :rating, :solvedCount)
                """, inserts);
        batch("UPDATE cf_problems SET name = :name, rating = :rating, solved_count = :solvedCount WHERE id = :id", updates);
    }

    private int syncTags(Map<String, CfProblem> byKey, Map<String, Long> ids) {
        Map<Long, Set<String>> current = new HashMap<>();
        jdbc.query("SELECT problem_id, tag FROM cf_problem_tags", new MapSqlParameterSource(),
                rs -> { current.computeIfAbsent(rs.getLong("problem_id"), k -> new HashSet<>()).add(rs.getString("tag")); });

        List<MapSqlParameterSource> added = new ArrayList<>();
        List<MapSqlParameterSource> removed = new ArrayList<>();
        for (Map.Entry<String, CfProblem> e : byKey.entrySet()) {
            Long id = ids.get(e.getKey());
            if (id == null) continue;
            Set<String> wanted = new HashSet<>();
            if (e.getValue().getTags() != null) {
                for (String tag : e.getValue().getTags()) {
                    if (tag != null && !tag.isBlank()) wanted.add(truncate(tag, 64));
                }
            }
            Set<String> have = current.getOrDefault(id, Set.of());
            for (String tag : wanted) {
                if (!have.contains(tag)) added.add(new MapSqlParameterSource("problemId", id).addValue("tag", tag));
            }
            for (String tag : have) {
                if (!wanted.contains(tag)) removed.add(new MapSqlParameterSource("problemId", id).addValue("tag", tag));
            }
        }

        batch("DELETE FROM cf_problem_tags WHERE problem_id = :problemId AND tag = :tag", removed);
        batch("INSERT INTO cf_problem_tags (problem_id, tag) VALUES (:problemId, :tag)", added);
        return added.size() + removed.size();
    }

    private void batch(String sql, List<MapSqlParameterSource> rows) {
        for (int start = 0; start < rows.size(); start += BATCH) {
            List<MapSqlParameterSource> chunk = rows.subList(start, Math.min(start + BATCH, rows.size()));
            jdbc.batchUpdate(sql, chunk.toArray(new MapSqlParameterSource[0]));
        }
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
