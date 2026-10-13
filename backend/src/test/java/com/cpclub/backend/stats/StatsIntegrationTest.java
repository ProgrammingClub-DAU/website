package com.cpclub.backend.stats;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import com.cpclub.backend.stats.dto.*;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.stats.entity.PlatformDailyTotal;
import com.cpclub.backend.stats.service.StatsService;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.common.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class StatsIntegrationTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private EntityManager entityManager;

    private User userA;
    private User userB;
    private User userC;

    @BeforeEach
    void setUp() {
        userA = User.builder()
                .name("Alice")
                .email("alice@dau.ac.in")
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.CORE)
                .codeforcesHandle("alice_cf")
                .leetcodeHandle("alice_lc")
                .leetcodeTotalSolved(112)
                .leetcodeEasySolved(50)
                .leetcodeMediumSolved(50)
                .leetcodeHardSolved(12)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(userA);

        userB = User.builder()
                .name("Bob")
                .email("bob@dau.ac.in")
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.STUDENT)
                .codeforcesHandle("bob_cf")
                .leetcodeHandle("bob_lc")
                .leetcodeTotalSolved(80)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(userB);

        userC = User.builder()
                .name("Charlie")
                .email("charlie@dau.ac.in")
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.BATCH_REPRESENTATIVE)
                .codeforcesHandle("charlie_cf")
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(userC);

        entityManager.flush();
    }

    private CfProblem createProblem(int contestId, String index, Integer rating) {
        CfProblem p = CfProblem.builder()
                .problemKey(contestId + "/" + index)
                .contestId(contestId)
                .problemIndex(index)
                .name("Problem " + index)
                .rating(rating)
                .build();
        entityManager.persist(p);
        return p;
    }

    private CfSolve createSolve(User user, CfProblem problem, LocalDateTime acAt) {
        CfSolve s = CfSolve.builder()
                .user(user)
                .problem(problem)
                .firstAcAt(acAt)
                .firstAcSubmissionId(System.nanoTime())
                .participantType("PRACTICE")
                .build();
        entityManager.persist(s);
        return s;
    }

    private ContestParticipation createParticipation(User user, Platform platform, String contestId, String name,
                                                     LocalDateTime startedAt, boolean rated, Integer rank, Integer newRating) {
        ContestParticipation cp = ContestParticipation.builder()
                .user(user)
                .platform(platform)
                .externalContestId(contestId)
                .contestName(name)
                .startedAt(startedAt)
                .rated(rated)
                .contestRank(rank)
                .newRating(newRating)
                .build();
        entityManager.persist(cp);
        return cp;
    }

    private PlatformDailyTotal createDailyTotal(User user, LocalDate date, int solved) {
        PlatformDailyTotal dt = PlatformDailyTotal.builder()
                .user(user)
                .platform("LEETCODE")
                .capturedOn(date)
                .totalSolved(solved)
                .build();
        entityManager.persist(dt);
        return dt;
    }

    @Test
    @DisplayName("cfSolved_excludesGymProblems: One problemset and one gym solve yields solved = 1")
    void cfSolved_excludesGymProblems() {
        CfProblem regularProblem = createProblem(1800, "A", 1200);
        CfProblem gymProblem = createProblem(100500, "B", 1500); // contest_id >= 100000

        createSolve(userA, regularProblem, LocalDateTime.now(ZoneId.of("UTC")));
        createSolve(userA, gymProblem, LocalDateTime.now(ZoneId.of("UTC")));
        entityManager.flush();

        MemberStatsDto stats = statsService.getMemberStats(userA.getId(), StatsPeriod.ALL_TIME);
        PlatformStatsDto cfStats = stats.getPlatforms().stream()
                .filter(p -> p.getPlatform() == StatsPlatform.CODEFORCES)
                .findFirst()
                .orElseThrow();

        assertThat(cfStats.getSolved()).isEqualTo(1);
    }

    @Test
    @DisplayName("avgRating_nullBelowTenRatedSolves: 9 rated solves plus 5 unrated is null and absent from AVG board")
    void avgRating_nullBelowTenRatedSolves() {
        // 9 rated solves
        for (int i = 1; i <= 9; i++) {
            CfProblem p = createProblem(1700, "A" + i, 1400);
            createSolve(userA, p, LocalDateTime.now(ZoneId.of("UTC")));
        }
        // 5 unrated solves
        for (int i = 1; i <= 5; i++) {
            CfProblem p = createProblem(1700, "U" + i, null);
            createSolve(userA, p, LocalDateTime.now(ZoneId.of("UTC")));
        }
        entityManager.flush();

        // 1. Member stats should have avgProblemRating = null
        MemberStatsDto stats = statsService.getMemberStats(userA.getId(), StatsPeriod.ALL_TIME);
        PlatformStatsDto cfStats = stats.getPlatforms().stream()
                .filter(p -> p.getPlatform() == StatsPlatform.CODEFORCES)
                .findFirst()
                .orElseThrow();
        assertThat(cfStats.getAvgProblemRating()).isNull();
        assertThat(cfStats.getRatedSolvesCount()).isEqualTo(9);

        // 2. Leaderboard for AVG_PROBLEM_RATING should omit userA
        StatsLeaderboardResponseDto lb = statsService.getLeaderboard(
                StatsMetric.AVG_PROBLEM_RATING, StatsPlatform.CODEFORCES, StatsPeriod.ALL_TIME, LeaderboardFilter.ALL, 0, 50);
        assertThat(lb.getEntries().stream().noneMatch(e -> e.getUserId().equals(userA.getId()))).isTrue();

        // Now add the 10th rated solve
        CfProblem tenth = createProblem(1700, "A10", 1600);
        createSolve(userA, tenth, LocalDateTime.now(ZoneId.of("UTC")));
        entityManager.flush();

        MemberStatsDto statsAfter = statsService.getMemberStats(userA.getId(), StatsPeriod.ALL_TIME);
        PlatformStatsDto cfStatsAfter = statsAfter.getPlatforms().stream()
                .filter(p -> p.getPlatform() == StatsPlatform.CODEFORCES)
                .findFirst()
                .orElseThrow();
        assertThat(cfStatsAfter.getAvgProblemRating()).isNotNull();
        assertThat(cfStatsAfter.getRatedSolvesCount()).isEqualTo(10);

        StatsLeaderboardResponseDto lbAfter = statsService.getLeaderboard(
                StatsMetric.AVG_PROBLEM_RATING, StatsPlatform.CODEFORCES, StatsPeriod.ALL_TIME, LeaderboardFilter.ALL, 0, 50);
        assertThat(lbAfter.getEntries().stream().anyMatch(e -> e.getUserId().equals(userA.getId()))).isTrue();
    }

    @Test
    @DisplayName("lcSolved_liveMinusBaseline: Baseline 100 on Monday, live total 112 -> WEEK solved = 12")
    void lcSolved_liveMinusBaseline() {
        // userA has leetcodeTotalSolved = 112
        LocalDate todayIst = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate startOfWeekIst = todayIst.minusDays(todayIst.getDayOfWeek().getValue() - 1);
        createDailyTotal(userA, startOfWeekIst, 100);
        entityManager.flush();

        MemberStatsDto stats = statsService.getMemberStats(userA.getId(), StatsPeriod.WEEK);
        PlatformStatsDto lcStats = stats.getPlatforms().stream()
                .filter(p -> p.getPlatform() == StatsPlatform.LEETCODE)
                .findFirst()
                .orElseThrow();

        assertThat(lcStats.getSolved()).isEqualTo(12);
    }

    @Test
    @DisplayName("lcSolved_noBaseline: No row on or before period start -> Null, response carries trackingSince")
    void lcSolved_noBaseline() {
        // userA has no daily total at all before period start
        MemberStatsDto stats = statsService.getMemberStats(userA.getId(), StatsPeriod.WEEK);
        PlatformStatsDto lcStats = stats.getPlatforms().stream()
                .filter(p -> p.getPlatform() == StatsPlatform.LEETCODE)
                .findFirst()
                .orElseThrow();

        assertThat(lcStats.getSolved()).isNull();

        // Now add a baseline in the future or after start
        LocalDate tomorrowIst = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(1);
        createDailyTotal(userA, tomorrowIst, 105);
        entityManager.flush();

        MemberStatsDto statsWithFuture = statsService.getMemberStats(userA.getId(), StatsPeriod.WEEK);
        PlatformStatsDto lcStatsWithFuture = statsWithFuture.getPlatforms().stream()
                .filter(p -> p.getPlatform() == StatsPlatform.LEETCODE)
                .findFirst()
                .orElseThrow();

        assertThat(lcStatsWithFuture.getSolved()).isNull();
        assertThat(lcStatsWithFuture.getTrackingSince()).isEqualTo(tomorrowIst);
    }

    @Test
    @DisplayName("leaderboard_tiesShareRank: Two members on 7 solves both rank 1; next is rank 3")
    void leaderboard_tiesShareRank() {
        // User A: 7 solves
        for (int i = 1; i <= 7; i++) {
            CfProblem p = createProblem(1600, "P" + i, 1000);
            createSolve(userA, p, LocalDateTime.now(ZoneId.of("UTC")));
        }
        // User B: 7 solves
        for (int i = 1; i <= 7; i++) {
            CfProblem p = createProblem(1601, "P" + i, 1000);
            createSolve(userB, p, LocalDateTime.now(ZoneId.of("UTC")));
        }
        // User C: 4 solves
        for (int i = 1; i <= 4; i++) {
            CfProblem p = createProblem(1602, "P" + i, 1000);
            createSolve(userC, p, LocalDateTime.now(ZoneId.of("UTC")));
        }
        entityManager.flush();

        StatsLeaderboardResponseDto lb = statsService.getLeaderboard(
                StatsMetric.SOLVED, StatsPlatform.CODEFORCES, StatsPeriod.ALL_TIME, LeaderboardFilter.ALL, 0, 50);

        List<StatsLeaderboardEntryDto> entries = lb.getEntries();
        assertThat(entries).hasSize(3);

        StatsLeaderboardEntryDto entry1 = entries.get(0);
        StatsLeaderboardEntryDto entry2 = entries.get(1);
        StatsLeaderboardEntryDto entry3 = entries.get(2);

        assertThat(entry1.getRank()).isEqualTo(1);
        assertThat(entry2.getRank()).isEqualTo(1);
        assertThat(entry3.getRank()).isEqualTo(3);
    }

    @Test
    @DisplayName("leaderboard_omitsMembersWithoutData: Member with no CF handle is omitted")
    void leaderboard_omitsMembersWithoutData() {
        User noHandleUser = User.builder()
                .name("NoHandle")
                .email("nohandle@dau.ac.in")
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.STUDENT)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(noHandleUser);
        entityManager.flush();

        StatsLeaderboardResponseDto lb = statsService.getLeaderboard(
                StatsMetric.SOLVED, StatsPlatform.CODEFORCES, StatsPeriod.ALL_TIME, LeaderboardFilter.ALL, 0, 50);

        assertThat(lb.getEntries().stream().noneMatch(e -> e.getUserId().equals(noHandleUser.getId()))).isTrue();
    }

    @Test
    @DisplayName("leaderboard_clubFilterApplies: filter=CORE returns only CORE and ASSOCIATE_CORE")
    void leaderboard_clubFilterApplies() {
        // userA is CORE
        // userB is STUDENT
        // userC is BATCH_REPRESENTATIVE
        CfProblem p1 = createProblem(1900, "A", 1000);
        createSolve(userA, p1, LocalDateTime.now(ZoneId.of("UTC")));
        createSolve(userB, p1, LocalDateTime.now(ZoneId.of("UTC")));
        createSolve(userC, p1, LocalDateTime.now(ZoneId.of("UTC")));
        entityManager.flush();

        StatsLeaderboardResponseDto lbCore = statsService.getLeaderboard(
                StatsMetric.SOLVED, StatsPlatform.CODEFORCES, StatsPeriod.ALL_TIME, LeaderboardFilter.CORE, 0, 50);

        assertThat(lbCore.getEntries()).hasSize(1);
        assertThat(lbCore.getEntries().get(0).getUserId()).isEqualTo(userA.getId());
    }

    @Test
    @DisplayName("leaderboard_rejectsInvalidCombination: AVG_PROBLEM_RATING with LEETCODE returns 400")
    void leaderboard_rejectsInvalidCombination() {
        assertThatThrownBy(() -> statsService.getLeaderboard(
                StatsMetric.AVG_PROBLEM_RATING, StatsPlatform.LEETCODE, StatsPeriod.ALL_TIME, LeaderboardFilter.ALL, 0, 50))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> statsService.getLeaderboard(
                StatsMetric.ATTENDANCE, StatsPlatform.CODEFORCES, StatsPeriod.MONTH, LeaderboardFilter.ALL, 0, 50))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> statsService.getLeaderboard(
                StatsMetric.ATTENDANCE, StatsPlatform.OVERALL, StatsPeriod.WEEK, LeaderboardFilter.ALL, 0, 50))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("attendance_countsCompletedEventsOnly: Attendee rows on CANCELLED and COMPLETED -> Count = 1")
    void attendance_countsCompletedEventsOnly() {
        Event completed = Event.builder()
                .title("Workshop")
                .location("Lab 1")
                .eventDate(LocalDateTime.now().minusDays(2))
                .status(EventStatus.COMPLETED)
                .createdBy(userA)
                .build();
        entityManager.persist(completed);

        Event cancelled = Event.builder()
                .title("Contest")
                .location("Lab 2")
                .eventDate(LocalDateTime.now().minusDays(1))
                .status(EventStatus.CANCELLED)
                .createdBy(userA)
                .build();
        entityManager.persist(cancelled);

        EventAttendee ea1 = EventAttendee.builder()
                .event(completed)
                .user(userA)
                .addedBy(userA)
                .addedAt(LocalDateTime.now())
                .build();
        entityManager.persist(ea1);

        EventAttendee ea2 = EventAttendee.builder()
                .event(cancelled)
                .user(userA)
                .addedBy(userA)
                .addedAt(LocalDateTime.now())
                .build();
        entityManager.persist(ea2);
        entityManager.flush();

        StatsLeaderboardResponseDto lb = statsService.getLeaderboard(
                StatsMetric.ATTENDANCE, StatsPlatform.OVERALL, StatsPeriod.ALL_TIME, LeaderboardFilter.ALL, 0, 50);

        assertThat(lb.getEntries()).hasSize(1);
        assertThat(lb.getEntries().get(0).getUserId()).isEqualTo(userA.getId());
        assertThat(lb.getEntries().get(0).getValue()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("compare_headToHead: Three shared rated contests, A ahead in two -> aAhead = 2, bAhead = 1")
    void compare_headToHead() {
        LocalDateTime t1 = LocalDateTime.now(ZoneId.of("UTC")).minusDays(10);
        LocalDateTime t2 = LocalDateTime.now(ZoneId.of("UTC")).minusDays(5);
        LocalDateTime t3 = LocalDateTime.now(ZoneId.of("UTC")).minusDays(1);

        // Contest 1: A rank 100, B rank 200 (A ahead)
        createParticipation(userA, Platform.CODEFORCES, "c1", "Contest 1", t1, true, 100, 1600);
        createParticipation(userB, Platform.CODEFORCES, "c1", "Contest 1", t1, true, 200, 1500);

        // Contest 2: A rank 300, B rank 150 (B ahead)
        createParticipation(userA, Platform.CODEFORCES, "c2", "Contest 2", t2, true, 300, 1580);
        createParticipation(userB, Platform.CODEFORCES, "c2", "Contest 2", t2, true, 150, 1550);

        // Contest 3: A rank 50, B rank 80 (A ahead)
        createParticipation(userA, Platform.CODEFORCES, "c3", "Contest 3", t3, true, 50, 1620);
        createParticipation(userB, Platform.CODEFORCES, "c3", "Contest 3", t3, true, 80, 1540);

        entityManager.flush();

        CompareDto comp = statsService.compare(userA.getId(), userB.getId());

        assertThat(comp.getHeadToHead()).isNotNull();
        assertThat(comp.getHeadToHead().getAAhead()).isEqualTo(2);
        assertThat(comp.getHeadToHead().getBAhead()).isEqualTo(1);
        assertThat(comp.getHeadToHead().getTied()).isEqualTo(0);
        assertThat(comp.getHeadToHead().getRecentContests()).hasSize(3);
    }
}
