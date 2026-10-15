package com.cpclub.backend.score;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.score.entity.MemberScore;
import com.cpclub.backend.score.repository.MemberScoreRepository;
import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MemberScoreServiceTest {

    @Autowired
    private MemberScoreService memberScoreService;

    @Autowired
    private MemberScoreRepository memberScoreRepository;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String name, String email, Integer rating, Integer lcRating, Integer lcSolved) {
        User u = User.builder()
                .name(name)
                .email(email)
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.STUDENT)
                .rating(rating)
                .leetcodeRating(lcRating)
                .leetcodeTotalSolved(lcSolved)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(u);
        return u;
    }

    private CfProblem createProblem(int contestId, String index) {
        CfProblem p = CfProblem.builder()
                .problemKey(contestId + "/" + index)
                .contestId(contestId)
                .problemIndex(index)
                .name("Problem " + index)
                .rating(1200)
                .build();
        entityManager.persist(p);
        return p;
    }

    private CfSolve createSolve(User user, CfProblem problem) {
        CfSolve s = CfSolve.builder()
                .user(user)
                .problem(problem)
                .firstAcAt(LocalDateTime.now(ZoneId.of("UTC")))
                .firstAcSubmissionId(System.nanoTime())
                .participantType("PRACTICE")
                .build();
        entityManager.persist(s);
        return s;
    }

    private void createContest(User user, Platform platform, String contestId, LocalDateTime startedAt) {
        ContestParticipation cp = ContestParticipation.builder()
                .user(user)
                .platform(platform)
                .externalContestId(contestId)
                .contestName("Contest " + contestId)
                .startedAt(startedAt)
                .rated(true)
                .contestRank(100)
                .newRating(1500)
                .oldRating(1400)
                .build();
        entityManager.persist(cp);
    }

    @Test
    @DisplayName("Three members with equal solved totals get equal solved_score")
    void threeMembersWithEqualSolvedTotals_getEqualSolvedScore() {
        User u1 = createUser("Alice", "alice@dau.ac.in", 1500, null, 10);
        User u2 = createUser("Bob", "bob@dau.ac.in", 1600, null, 10);
        User u3 = createUser("Charlie", "charlie@dau.ac.in", 1700, null, 10);

        entityManager.flush();

        memberScoreService.computeScores();

        MemberScore ms1 = memberScoreRepository.findById(u1.getId()).orElseThrow();
        MemberScore ms2 = memberScoreRepository.findById(u2.getId()).orElseThrow();
        MemberScore ms3 = memberScoreRepository.findById(u3.getId()).orElseThrow();

        assertThat(ms1.getSolvedScore()).isEqualTo(ms2.getSolvedScore());
        assertThat(ms2.getSolvedScore()).isEqualTo(ms3.getSolvedScore());
    }

    @Test
    @DisplayName("Only one member has a LeetCode rating -> that component = 1000")
    void onlyOneMemberHasLeetCodeRating_thatComponentIs1000() {
        User u1 = createUser("Alice", "alice@dau.ac.in", null, 1850, 50);
        entityManager.flush();

        memberScoreService.computeScores();

        MemberScore ms1 = memberScoreRepository.findById(u1.getId()).orElseThrow();
        assertThat(ms1.getRatingScore()).isEqualTo(1000);
    }

    @Test
    @DisplayName("Strong on CF only, versus average on two platforms -> rating score uses MAX, not mean")
    void strongOnCfOnly_versusAverageOnTwoPlatforms_ratingScoreUsesMax() {
        // u1 is top rated on CF, but has no LC rating
        User u1 = createUser("Alice", "alice@dau.ac.in", 2200, null, 100);
        // u2 is median rated on CF and median rated on LC
        User u2 = createUser("Bob", "bob@dau.ac.in", 1600, 1600, 100);
        // u3 is lower rated on CF and higher rated on LC to create a distribution
        User u3 = createUser("Charlie", "charlie@dau.ac.in", 1200, 2000, 100);

        entityManager.flush();

        memberScoreService.computeScores();

        MemberScore ms1 = memberScoreRepository.findById(u1.getId()).orElseThrow();
        MemberScore ms2 = memberScoreRepository.findById(u2.getId()).orElseThrow();

        // Alice is top (rank 1) on CF -> ratingScore 1000
        assertThat(ms1.getRatingScore()).isEqualTo(1000);
        // Bob is middle on both -> ratingScore ~500, definitely less than Alice
        assertThat(ms2.getRatingScore()).isLessThan(ms1.getRatingScore());
    }

    @Test
    @DisplayName("Member with no platform data gets no member_scores row")
    void memberWithNoPlatformData_getsNoMemberScoresRow() {
        // User with no rating, no solves, no contests
        User emptyUser = createUser("Ghost", "ghost@dau.ac.in", null, null, null);
        // Active user so computeScores has work to do
        User activeUser = createUser("Active", "active@dau.ac.in", 1500, null, 20);

        entityManager.flush();

        memberScoreService.computeScores();

        Optional<MemberScore> ghostScore = memberScoreRepository.findById(emptyUser.getId());
        assertThat(ghostScore).isEmpty();

        Optional<MemberScore> activeScore = memberScoreRepository.findById(activeUser.getId());
        assertThat(activeScore).isPresent();
    }
}
