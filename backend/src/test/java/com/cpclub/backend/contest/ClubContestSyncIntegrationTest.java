package com.cpclub.backend.contest;

import com.cpclub.backend.contest.client.CfStandings;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestKind;
import com.cpclub.backend.contest.entity.ClubContestProblem;
import com.cpclub.backend.contest.entity.ClubContestResult;
import com.cpclub.backend.contest.repository.ClubContestProblemRepository;
import com.cpclub.backend.contest.repository.ClubContestProblemResultRepository;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.contest.service.ClubContestSyncService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * The standings sync against real PostgreSQL with Flyway on (D22): the bulk deletes,
 * the composite-key per-problem rows and the unique constraints are what is under
 * test, and the H2 schema generated from the entities has none of the migration's
 * constraints.
 */
@Transactional
class ClubContestSyncIntegrationTest extends PostgresIntegrationTest {

    @MockitoBean
    private CodeforcesAuthorizedClient codeforces;

    @Autowired private ClubContestSyncService syncService;
    @Autowired private ClubContestRepository contestRepository;
    @Autowired private ClubContestProblemRepository problemRepository;
    @Autowired private ClubContestResultRepository resultRepository;
    @Autowired private ClubContestProblemResultRepository problemResultRepository;
    @Autowired private UserRepository userRepository;

    private ClubContest contest;

    @BeforeEach
    void setUp() {
        User admin = userRepository.save(User.builder().name("Admin").email("admin-sync@dau.ac.in").build());
        member("Alice", "alice");
        member("Bob", "Bob_CF");
        member("Carol", "carol");
        member("Dave", "dave");

        contest = contestRepository.save(ClubContest.builder()
                .cfContestId(102345)
                .kind(ClubContestKind.MASHUP)
                .name("Club Mashup #1")
                .startsAt(LocalDateTime.of(2026, 10, 10, 12, 0))
                .durationSeconds(7200)
                .createdBy(admin)
                .build());
        for (String index : List.of("A", "B", "C")) {
            problemRepository.save(ClubContestProblem.builder()
                    .clubContest(contest).problemIndex(index).name("Problem " + index).build());
        }
    }

    @Test
    @DisplayName("Practice rows are ignored")
    void practiceRowsIgnored() {
        standings(
                row("alice", "CONTESTANT", 2, 30, true, true, false),
                row("Bob_CF", "PRACTICE", 3, 0, true, true, true),
                row("carol", "VIRTUAL", 3, 0, true, true, true));

        int synced = syncService.sync(contest.getId());

        assertEquals(1, synced);
        List<ClubContestResult> results = resultRepository.findRanklist(contest.getId());
        assertEquals(List.of("Alice"), results.stream().map(r -> r.getUser().getName()).toList());
    }

    @Test
    @DisplayName("Two club members on equal points and penalty share a club rank")
    void equalPointsAndPenaltyShareARank() {
        standings(
                row("alice", "CONTESTANT", 2, 50, true, true, false),
                row("Bob_CF", "CONTESTANT", 2, 50, true, false, true),
                row("carol", "CONTESTANT", 1, 10, true, false, false));

        syncService.sync(contest.getId());

        Map<String, Integer> ranks = resultRepository.findRanklist(contest.getId()).stream()
                .collect(Collectors.toMap(r -> r.getUser().getName(), ClubContestResult::getContestRank));
        assertEquals(Map.of("Alice", 1, "Bob", 1, "Carol", 3), ranks);
    }

    @Test
    @DisplayName("Syncing twice leaves identical rows")
    void syncIsIdempotent() {
        standings(
                row("alice", "CONTESTANT", 3, 70, true, true, true),
                row("carol", "OUT_OF_COMPETITION", 1, 5, true, false, false));

        syncService.sync(contest.getId());
        List<String> first = snapshot();
        syncService.sync(contest.getId());
        List<String> second = snapshot();

        assertEquals(first, second);
        assertEquals(2, resultRepository.findRanklist(contest.getId()).size());
        assertEquals(4, problemResultRepository.findByContestIds(List.of(contest.getId())).size(),
                "3 for Alice and 1 for Carol, not doubled");
    }

    @Test
    @DisplayName("A handle in a different case from the stored one still matches")
    void handleCaseDiffers() {
        standings(row("bob_cf", "CONTESTANT", 1, 12, true, false, false));

        syncService.sync(contest.getId());

        List<ClubContestResult> results = resultRepository.findRanklist(contest.getId());
        assertEquals(1, results.size());
        assertEquals("Bob", results.get(0).getUser().getName());
    }

    @Test
    @DisplayName("Per-problem outcomes record solved and attempted-but-unsolved")
    void perProblemOutcomes() {
        CfStandings.Row r = row("alice", "CONTESTANT", 1, 20, true, false, false);
        r.getProblemResults().get(1).setRejectedAttemptCount(2); // B: tried, not solved
        standings(r);

        syncService.sync(contest.getId());

        Map<String, Boolean> outcomes = problemResultRepository.findByContestIds(List.of(contest.getId())).stream()
                .collect(Collectors.toMap(pr -> pr.getProblem().getProblemIndex(), pr -> pr.isSolvedInContest()));
        assertEquals(Map.of("A", true, "B", false), outcomes, "C was not attempted, so it has no row");

        ClubContestResult result = resultRepository.findRanklist(contest.getId()).get(0);
        assertEquals(new BigDecimal("1.00"), result.getCfPoints());
        assertEquals(1, result.getSolvedCount());
        assertNotNull(contestRepository.findById(contest.getId()).orElseThrow().getLastSyncedAt());
    }

    // ------------------------------------------------------------------ helpers

    private void member(String name, String handle) {
        userRepository.save(User.builder().name(name).email(name.toLowerCase() + "-sync@dau.ac.in")
                .codeforcesHandle(handle).build());
    }

    private List<String> snapshot() {
        return resultRepository.findRanklist(contest.getId()).stream()
                .map(r -> r.getUser().getName() + "|" + r.getContestRank() + "|" + r.getCfPoints()
                        + "|" + r.getPenalty() + "|" + r.getSolvedCount())
                .toList();
    }

    private void standings(CfStandings.Row... rows) {
        CfStandings s = new CfStandings();
        for (String index : List.of("A", "B", "C")) {
            CfStandings.Problem p = new CfStandings.Problem();
            p.setIndex(index);
            s.getProblems().add(p);
        }
        s.setRows(List.of(rows));
        when(codeforces.contestStandings(eq(102345), anyCollection())).thenReturn(s);
    }

    private static CfStandings.Row row(String handle, String type, double points, int penalty, boolean... solved) {
        CfStandings.Member m = new CfStandings.Member();
        m.setHandle(handle);
        CfStandings.Party party = new CfStandings.Party();
        party.setMembers(List.of(m));
        party.setParticipantType(type);
        CfStandings.Row row = new CfStandings.Row();
        row.setParty(party);
        row.setPoints(points);
        row.setPenalty(penalty);
        for (boolean s : solved) {
            CfStandings.ProblemResult pr = new CfStandings.ProblemResult();
            pr.setPoints(s ? 1.0 : 0.0);
            pr.setRejectedAttemptCount(0);
            row.getProblemResults().add(pr);
        }
        return row;
    }
}
