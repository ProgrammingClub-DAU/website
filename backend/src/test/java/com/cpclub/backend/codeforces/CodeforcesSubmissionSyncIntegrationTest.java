package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfAuthor;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.codeforces.service.CodeforcesSubmissionSyncService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * A member's Codeforces solves, synced against real PostgreSQL and, as in
 * production, outside any transaction (so not {@code @Transactional}). Each page
 * is written with batched upserts, which is what is under test: the earliest
 * accepted submission per problem wins, across pages and across syncs, and a
 * stored problem gains a rating it lacked.
 */
class CodeforcesSubmissionSyncIntegrationTest extends PostgresIntegrationTest {

    private static final String HANDLE = "bulk_sync_member";
    private static final List<String> KEYS = List.of("990001/A", "990001/B", "990001/C", "990001/D");

    @MockitoBean
    private CodeforcesApiClient codeforces;

    @Autowired private CodeforcesSubmissionSyncService syncService;
    @Autowired private CfProblemRepository problemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    private User member;

    @BeforeEach
    void setUp() {
        member = userRepository.save(User.builder().name("Bulk Sync").email("bulk-sync@dau.ac.in")
                .codeforcesHandle(HANDLE).build());

        // Known from the problemset sync, before Codeforces published its rating.
        CfProblem known = new CfProblem();
        known.setProblemKey("990001/B");
        known.setContestId(990001);
        known.setProblemIndex("B");
        known.setName("Beta");
        known.setSolvedCount(0);
        problemRepository.save(known);
    }

    @AfterEach
    void tearDown() {
        jdbc.update("DELETE FROM cf_solves WHERE user_id = :id", new MapSqlParameterSource("id", member.getId()));
        jdbc.update("DELETE FROM cf_problems WHERE problem_key IN (:keys)", new MapSqlParameterSource("keys", KEYS));
        userRepository.deleteById(member.getId());
    }

    @Test
    @DisplayName("First sync keeps each problem's earliest accepted submission, across pages")
    void firstSync() {
        // Newest first, as Codeforces returns them; a first sync pages 500 at a time.
        when(codeforces.userStatus(HANDLE, 1, 500)).thenReturn(List.of(
                sub(106, "A", "OK", 6000, "PRACTICE", null),
                sub(105, "B", "WRONG_ANSWER", 5000, "PRACTICE", null),
                sub(104, "A", "OK", 4000, "VIRTUAL", null),
                sub(103, "B", "OK", 3000, "PRACTICE", 1500),
                sub(102, null, "OK", 2500, "PRACTICE", null)));
        when(codeforces.userStatus(HANDLE, 501, 500)).thenReturn(List.of(
                sub(101, "A", "OK", 1000, "PRACTICE", null),
                sub(100, "C", "OK", 900, "PRACTICE", 800)));

        syncService.syncMember(member);

        Map<String, Solve> solves = solves();
        assertEquals(3, solves.size());
        assertEquals(new Solve(at(1000), 101, "PRACTICE"), solves.get("990001/A"));
        assertEquals(new Solve(at(3000), 103, "PRACTICE"), solves.get("990001/B"));
        assertEquals(new Solve(at(900), 100, "PRACTICE"), solves.get("990001/C"));

        CfProblem known = problemRepository.findByProblemKey("990001/B").orElseThrow();
        assertEquals(1500, known.getRating());
        assertEquals("Beta", known.getName());
        CfProblem created = problemRepository.findByProblemKey("990001/C").orElseThrow();
        assertEquals("Problem 990001C", created.getName());
        assertEquals(800, created.getRating());
        assertEquals(0, created.getSolvedCount());

        User synced = userRepository.findById(member.getId()).orElseThrow();
        assertEquals(106L, synced.getCfLastSubmissionId());
        assertNotNull(synced.getCfSyncedAt());
    }

    @Test
    @DisplayName("Later syncs add new solves and leave earlier ones as they were")
    void incrementalSync() {
        when(codeforces.userStatus(HANDLE, 1, 500)).thenReturn(List.of(
                sub(101, "A", "OK", 1000, "PRACTICE", 1200)));
        syncService.syncMember(member);

        // Incremental: 100 at a time, stopping at the last submission already seen.
        // A later accepted submission of A must not replace the earlier one, and a
        // rating already stored is not overwritten.
        when(codeforces.userStatus(HANDLE, 1, 100)).thenReturn(List.of(
                sub(108, "D", "OK", 8000, "VIRTUAL", null),
                sub(107, "A", "OK", 7000, "PRACTICE", 1900),
                sub(101, "A", "OK", 1000, "PRACTICE", 1200)));
        syncService.syncMember(userRepository.findById(member.getId()).orElseThrow());

        Map<String, Solve> solves = solves();
        assertEquals(2, solves.size());
        assertEquals(new Solve(at(1000), 101, "PRACTICE"), solves.get("990001/A"));
        assertEquals(new Solve(at(8000), 108, "VIRTUAL"), solves.get("990001/D"));
        assertEquals(1200, problemRepository.findByProblemKey("990001/A").orElseThrow().getRating());
        assertEquals(108L, userRepository.findById(member.getId()).orElseThrow().getCfLastSubmissionId());
    }

    private record Solve(LocalDateTime firstAcAt, long submissionId, String participantType) {}

    private Map<String, Solve> solves() {
        Map<String, Solve> byKey = new HashMap<>();
        jdbc.query("""
                SELECT p.problem_key, s.first_ac_at, s.first_ac_submission_id, s.participant_type
                FROM cf_solves s JOIN cf_problems p ON p.id = s.problem_id
                WHERE s.user_id = :id
                """, new MapSqlParameterSource("id", member.getId()),
                rs -> {
                    byKey.put(rs.getString("problem_key"), new Solve(
                            rs.getObject("first_ac_at", LocalDateTime.class),
                            rs.getLong("first_ac_submission_id"),
                            rs.getString("participant_type")));
                });
        return byKey;
    }

    private static LocalDateTime at(long epochSeconds) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
    }

    private static CfSubmission sub(long id, String index, String verdict, long time,
                                    String participantType, Integer rating) {
        com.cpclub.backend.codeforces.dto.CfProblem problem = new com.cpclub.backend.codeforces.dto.CfProblem();
        problem.setContestId(990001);
        problem.setIndex(index);
        problem.setName(index == null ? null : "Problem 990001" + index);
        problem.setRating(rating);

        CfAuthor author = new CfAuthor();
        author.setParticipantType(participantType);

        CfSubmission sub = new CfSubmission();
        sub.setId(id);
        sub.setContestId(990001);
        sub.setCreationTimeSeconds(time);
        sub.setProblem(problem);
        sub.setVerdict(verdict);
        sub.setAuthor(author);
        return sub;
    }
}
