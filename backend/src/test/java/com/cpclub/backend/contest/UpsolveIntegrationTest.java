package com.cpclub.backend.contest;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.codeforces.repository.CfSolveRepository;
import com.cpclub.backend.contest.dto.MyUpsolvesDto;
import com.cpclub.backend.contest.dto.UpsolveMatrixDto;
import com.cpclub.backend.contest.dto.UpsolveStatus;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestKind;
import com.cpclub.backend.contest.entity.ClubContestProblem;
import com.cpclub.backend.contest.entity.ClubContestProblemResult;
import com.cpclub.backend.contest.entity.ClubContestProblemResultId;
import com.cpclub.backend.contest.entity.ClubContestResult;
import com.cpclub.backend.contest.repository.ClubContestProblemRepository;
import com.cpclub.backend.contest.repository.ClubContestProblemResultRepository;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.contest.service.UpsolveService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Upsolve status (D12) against real PostgreSQL.
 *
 * <p>The contest runs 12:00-14:00 UTC on 10 Oct 2026. Every case below hinges on which
 * side of 14:00 an accepted submission falls, and on which Codeforces contest it was
 * made in.</p>
 */
@Transactional
class UpsolveIntegrationTest extends PostgresIntegrationTest {

    private static final int GYM_ID = 102345;
    private static final int ORIGINAL_ID = 1352;
    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 10, 12, 0);
    private static final LocalDateTime END = START.plusHours(2);

    @Autowired private UpsolveService upsolveService;
    @Autowired private ClubContestRepository contestRepository;
    @Autowired private ClubContestProblemRepository problemRepository;
    @Autowired private ClubContestResultRepository resultRepository;
    @Autowired private ClubContestProblemResultRepository problemResultRepository;
    @Autowired private CfProblemRepository cfProblemRepository;
    @Autowired private CfSolveRepository cfSolveRepository;
    @Autowired private UserRepository userRepository;

    private ClubContest contest;
    private User alice;
    private User bob;
    private ClubContestProblem a;

    @BeforeEach
    void setUp() {
        User admin = userRepository.save(User.builder().name("Admin").email("admin-up@dau.ac.in").build());
        alice = userRepository.save(User.builder().name("Alice").email("alice-up@dau.ac.in").codeforcesHandle("alice").build());
        bob = userRepository.save(User.builder().name("Bob").email("bob-up@dau.ac.in").codeforcesHandle("bob").build());

        contest = contestRepository.save(ClubContest.builder()
                .cfContestId(GYM_ID).kind(ClubContestKind.MASHUP).name("Club Mashup #1")
                .startsAt(START).durationSeconds(7200).createdBy(admin).build());
        a = problemRepository.save(ClubContestProblem.builder().clubContest(contest).problemIndex("A").name("Alpha").build());
        problemRepository.save(ClubContestProblem.builder().clubContest(contest).problemIndex("B").name("Beta").build());

        // Both entered. Alice solved A during the contest.
        ClubContestResult aliceResult = result(alice, 1);
        result(bob, 2);
        problemResultRepository.save(new ClubContestProblemResult(
                new ClubContestProblemResultId(aliceResult.getId(), a.getId()), aliceResult, a, true));
    }

    @Test
    @DisplayName("An accept inside the contest after it ended is an upsolve")
    void acceptAfterEnd_isUpsolved() {
        solve(bob, GYM_ID, "A", END.plusHours(1));

        assertEquals(UpsolveStatus.UPSOLVED, cell(bob, "A"));
    }

    @Test
    @DisplayName("An accept inside the contest before it ended is not an upsolve")
    void acceptBeforeEnd_isNotAnUpsolve() {
        // Accepted during the contest, but not as a counted contestant -- so the
        // standings never marked it solved. It still is not an upsolve.
        solve(bob, GYM_ID, "B", END.minusMinutes(10));

        assertEquals(UpsolveStatus.UNSOLVED, cell(bob, "B"));
    }

    @Test
    @DisplayName("Solving the original problem in another contest does not count (D12)")
    void acceptInOriginalContest_isUnsolved() {
        solve(bob, ORIGINAL_ID, "B", END.plusDays(1));

        assertEquals(UpsolveStatus.UNSOLVED, cell(bob, "B"));
    }

    @Test
    @DisplayName("A problem solved in the contest stays SOLVED_IN_CONTEST")
    void solvedInContestWins() {
        assertEquals(UpsolveStatus.SOLVED_IN_CONTEST, cell(alice, "A"));
    }

    @Test
    @DisplayName("Upsolve rate counts only members who had something left to upsolve")
    void upsolveRate() {
        solve(bob, GYM_ID, "A", END.plusHours(1));

        UpsolveMatrixDto matrix = upsolveService.getContestUpsolve(contest.getId());

        // A: Alice solved it live, so only Bob could upsolve it -- and did.
        assertEquals(1.0, matrix.getProblems().get(0).getUpsolveRate());
        // B: neither solved it, neither upsolved it.
        assertEquals(0.0, matrix.getProblems().get(1).getUpsolveRate());
    }

    @Test
    @DisplayName("Everyone solving a problem live leaves no upsolve rate")
    void noRateWhenNothingLeft() {
        ClubContestResult bobResult = resultRepository.findRanklist(contest.getId()).stream()
                .filter(r -> r.getUser().getId().equals(bob.getId())).findFirst().orElseThrow();
        problemResultRepository.save(new ClubContestProblemResult(
                new ClubContestProblemResultId(bobResult.getId(), a.getId()), bobResult, a, true));

        assertNull(upsolveService.getContestUpsolve(contest.getId()).getProblems().get(0).getUpsolveRate());
    }

    @Test
    @DisplayName("Pending upsolves for a gym contest link under /gym/")
    void myUpsolves_gymLink() {
        MyUpsolvesDto mine = upsolveService.getMyUpsolves(bob.getId());

        assertEquals(1, mine.getContests().size());
        List<String> urls = mine.getContests().get(0).getPending().stream().map(MyUpsolvesDto.PendingProblem::getUrl).toList();
        assertEquals(List.of(
                "https://codeforces.com/gym/102345/problem/A",
                "https://codeforces.com/gym/102345/problem/B"), urls);
    }

    @Test
    @DisplayName("An upsolved problem leaves the pending list")
    void myUpsolves_dropsUpsolved() {
        solve(bob, GYM_ID, "A", END.plusHours(3));

        List<String> pending = upsolveService.getMyUpsolves(bob.getId()).getContests().get(0).getPending().stream()
                .map(MyUpsolvesDto.PendingProblem::getIndex).toList();

        assertEquals(List.of("B"), pending);
    }

    // ------------------------------------------------------------------ helpers

    private UpsolveStatus cell(User member, String index) {
        UpsolveMatrixDto matrix = upsolveService.getContestUpsolve(contest.getId());
        int col = matrix.getProblems().stream().map(UpsolveMatrixDto.Problem::getIndex).toList().indexOf(index);
        return matrix.getRows().stream()
                .filter(r -> r.getMember().getId().equals(member.getId()))
                .findFirst().orElseThrow()
                .getCells().get(col);
    }

    private ClubContestResult result(User user, int rank) {
        return resultRepository.save(ClubContestResult.builder()
                .clubContest(contest).user(user).contestRank(rank)
                .cfPoints(BigDecimal.ONE).penalty(0).solvedCount(0).build());
    }

    private void solve(User user, int cfContestId, String index, LocalDateTime at) {
        String key = cfContestId + index;
        CfProblem problem = cfProblemRepository.findByProblemKey(key).orElseGet(() ->
                cfProblemRepository.save(CfProblem.builder()
                        .problemKey(key).contestId(cfContestId).problemIndex(index).name("P " + key).build()));
        cfSolveRepository.save(CfSolve.builder()
                .user(user).problem(problem).firstAcAt(at)
                .firstAcSubmissionId((long) (at.hashCode() & 0x7fffffff)).participantType("PRACTICE").build());
    }
}
