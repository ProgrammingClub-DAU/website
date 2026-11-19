package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfAuthor;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.service.CodeforcesSubmissionSyncService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Times a new member's first Codeforces sync: every accepted submission in their
 * history written to {@code cf_problems} and {@code cf_solves} on real PostgreSQL.
 *
 * <p>The Codeforces API is stubbed, so this measures the application and database
 * work only. In production each page of 500 submissions also waits for one
 * rate-limited API call (0.5 requests/s).</p>
 *
 * <p>Two scenarios, because the cost differs: <b>new problems</b>, where every
 * solved problem is inserted too, and <b>known problems</b>, where the weekly
 * problemset sync has already stored them, as it usually has in production.</p>
 *
 * <p>Skipped unless {@code -Dbenchmark=true}, so it never slows the normal suite:</p>
 *
 * <pre>
 * ./mvnw test -Dtest=FirstSyncBenchmarkTest -Dbenchmark=true
 * </pre>
 *
 * <p>{@code -Dbenchmark.solves} and {@code -Dbenchmark.runs} change the size and the
 * number of timed runs (default 1,500 and 5). Each scenario does one untimed warm-up
 * run first. The file compiles against older versions of the sync too, so the same
 * benchmark can be run on the commit before a change to measure it.</p>
 */
@EnabledIfSystemProperty(named = "benchmark", matches = "true")
class FirstSyncBenchmarkTest extends PostgresIntegrationTest {

    private static final int SOLVES = Integer.getInteger("benchmark.solves", 1500);
    private static final int RUNS = Integer.getInteger("benchmark.runs", 5);
    private static final int PAGE = 500;

    /** Far above any real Codeforces contest id, so benchmark rows never meet real ones. */
    private static final int CONTEST_BASE = 900_000;
    private static final String[] INDEXES = {"A", "B", "C", "D", "E", "F"};

    @MockitoBean
    private CodeforcesApiClient codeforces;

    @Autowired private CodeforcesSubmissionSyncService syncService;
    @Autowired private UserRepository userRepository;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    private final List<CfSubmission> history = buildHistory(SOLVES);
    private int memberSeq;

    @Test
    void newProblems() {
        benchmark("new problems", false);
    }

    @Test
    void knownProblems() {
        benchmark("known problems", true);
    }

    private void benchmark(String scenario, boolean problemsKnown) {
        // Pages of the stubbed history, newest first, as Codeforces returns them.
        when(codeforces.userStatus(anyString(), anyInt(), anyInt())).thenAnswer(call -> {
            int from = call.getArgument(1);
            int count = call.getArgument(2);
            int start = Math.min(from - 1, history.size());
            return new ArrayList<>(history.subList(start, Math.min(start + count, history.size())));
        });

        runOnce(problemsKnown); // warm-up: JIT, connection pool, Hibernate metadata

        long[] millis = new long[RUNS];
        String fingerprint = null;
        for (int i = 0; i < RUNS; i++) {
            Result result = runOnce(problemsKnown);
            millis[i] = result.millis;
            fingerprint = result.fingerprint;
        }

        long[] sorted = millis.clone();
        Arrays.sort(sorted);
        System.out.printf("BENCH | %s | %d solves | runs (ms) %s | median %d ms | min %d | max %d | rows %s%n",
                scenario, SOLVES, Arrays.toString(millis), sorted[RUNS / 2], sorted[0], sorted[RUNS - 1],
                fingerprint);
    }

    private Result runOnce(boolean problemsKnown) {
        if (problemsKnown) {
            insertProblems();
        }
        User member = userRepository.save(User.builder()
                .name("Benchmark Member")
                .email("bench-" + (++memberSeq) + "-" + System.nanoTime() + "@dau.ac.in")
                .codeforcesHandle("bench_member_" + memberSeq + "_" + System.nanoTime())
                .build());

        long start = System.nanoTime();
        syncService.syncMember(member);
        long millis = (System.nanoTime() - start) / 1_000_000;

        MapSqlParameterSource byUser = new MapSqlParameterSource("id", member.getId());
        Integer stored = jdbc.queryForObject("SELECT COUNT(*) FROM cf_solves WHERE user_id = :id", byUser, Integer.class);
        assertEquals(SOLVES, stored, "every accepted submission should be stored as one solve");
        String fingerprint = fingerprint(member.getId());

        // Solves go with the member (ON DELETE CASCADE); then the benchmark's problems.
        jdbc.update("DELETE FROM users WHERE id = :id", byUser);
        jdbc.update("DELETE FROM cf_problems WHERE contest_id >= :base",
                new MapSqlParameterSource("base", CONTEST_BASE));
        return new Result(millis, fingerprint);
    }

    /** What the weekly problemset sync would already have stored. */
    private void insertProblems() {
        List<MapSqlParameterSource> rows = new ArrayList<>();
        for (CfSubmission sub : history) {
            rows.add(new MapSqlParameterSource()
                    .addValue("key", sub.getProblem().getContestId() + "/" + sub.getProblem().getIndex())
                    .addValue("contestId", sub.getProblem().getContestId())
                    .addValue("index", sub.getProblem().getIndex())
                    .addValue("name", sub.getProblem().getName())
                    .addValue("rating", sub.getProblem().getRating()));
        }
        jdbc.batchUpdate("""
                INSERT INTO cf_problems (problem_key, contest_id, problem_index, name, rating, solved_count)
                VALUES (:key, :contestId, :index, :name, :rating, 0)
                """, rows.toArray(new MapSqlParameterSource[0]));
    }

    /**
     * A hash of the member's stored solves (problem, submission, time), so runs on
     * different versions of the sync can be checked to store identical rows.
     */
    private String fingerprint(Long userId) {
        List<String> rows = jdbc.queryForList("""
                SELECT p.problem_key || ':' || s.first_ac_submission_id || ':' || s.first_ac_at
                FROM cf_solves s JOIN cf_problems p ON p.id = s.problem_id
                WHERE s.user_id = :id
                ORDER BY p.problem_key
                """, new MapSqlParameterSource("id", userId), String.class);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (String row : rows) {
                sha.update(row.getBytes(StandardCharsets.UTF_8));
                sha.update((byte) '\n');
            }
            return HexFormat.of().formatHex(sha.digest()).substring(0, 12);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** {@code solves} accepted submissions on distinct problems, newest first. */
    private static List<CfSubmission> buildHistory(int solves) {
        List<CfSubmission> subs = new ArrayList<>(solves);
        long firstSecond = 1_600_000_000L;
        for (int i = solves - 1; i >= 0; i--) {
            com.cpclub.backend.codeforces.dto.CfProblem problem = new com.cpclub.backend.codeforces.dto.CfProblem();
            problem.setContestId(CONTEST_BASE + i / INDEXES.length);
            problem.setIndex(INDEXES[i % INDEXES.length]);
            problem.setName("Benchmark problem " + i);
            problem.setRating(800 + (i % 28) * 100);

            CfAuthor author = new CfAuthor();
            author.setParticipantType("PRACTICE");

            CfSubmission sub = new CfSubmission();
            sub.setId(10_000_000L + i);
            sub.setContestId(problem.getContestId());
            sub.setCreationTimeSeconds(firstSecond + i * 600L);
            sub.setProblem(problem);
            sub.setVerdict("OK");
            sub.setAuthor(author);
            subs.add(sub);
        }
        return subs;
    }

    private record Result(long millis, String fingerprint) {}
}
