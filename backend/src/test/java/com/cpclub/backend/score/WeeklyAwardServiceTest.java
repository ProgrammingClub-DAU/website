package com.cpclub.backend.score;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.score.dto.WeeklyAwardDto;
import com.cpclub.backend.score.entity.AwardType;
import com.cpclub.backend.score.entity.WeeklyAward;
import com.cpclub.backend.score.repository.WeeklyAwardRepository;
import com.cpclub.backend.score.service.WeeklyAwardService;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.stats.entity.PlatformDailyTotal;
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class WeeklyAwardServiceTest {

    @Autowired
    private WeeklyAwardService weeklyAwardService;

    @Autowired
    private WeeklyAwardRepository weeklyAwardRepository;

    @Autowired
    private EntityManager entityManager;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private User createUser(String name, String email, Role role) {
        User u = User.builder()
                .name(name)
                .email(email)
                .role(role)
                .clubRole(ClubRole.STUDENT)
                .codeforcesHandle(name.toLowerCase() + "_cf")
                .leetcodeHandle(name.toLowerCase() + "_lc")
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

    private void createDailyTotal(User user, LocalDate date, int solved) {
        PlatformDailyTotal dt = PlatformDailyTotal.builder()
                .user(user)
                .platform("LEETCODE")
                .capturedOn(date)
                .totalSolved(solved)
                .easySolved(solved / 2)
                .mediumSolved(solved / 2)
                .hardSolved(0)
                .build();
        entityManager.persist(dt);
    }

    @Test
    @DisplayName("Solve at Monday 00:00:00 IST belongs to new week, not closed one")
    void solveAtMondayMidnightIst_belongsToNewWeekNotClosedOne() {
        // Closed week: Monday 2026-04-06 to Monday 2026-04-13
        LocalDate weekStart = LocalDate.of(2026, 4, 6);
        LocalDate weekEnd = LocalDate.of(2026, 4, 13);

        User u = createUser("Alice", "alice@dau.ac.in", Role.ROLE_USER);
        CfProblem p1 = createProblem(1001, "A");
        CfProblem p2 = createProblem(1001, "B");

        // Sunday night 23:59:59 IST (2026-04-12 18:29:59 UTC) -> Inside closed week
        LocalDateTime sundayNightUtc = LocalDateTime.ofInstant(
                Instant.parse("2026-04-12T18:29:59Z"), ZoneId.of("UTC"));
        createSolve(u, p1, sundayNightUtc);

        // Monday midnight 00:00:00 IST (2026-04-12 18:30:00 UTC) -> Belongs to NEW week!
        LocalDateTime mondayMidnightUtc = LocalDateTime.ofInstant(
                Instant.parse("2026-04-12T18:30:00Z"), ZoneId.of("UTC"));
        createSolve(u, p2, mondayMidnightUtc);

        entityManager.flush();

        weeklyAwardService.computeWeek(weekStart, null);

        List<WeeklyAwardDto> awards = weeklyAwardService.getAwardsByWeek(weekStart);
        WeeklyAwardDto mostSolved = awards.stream()
                .filter(a -> a.getAwardType() == AwardType.MOST_SOLVED)
                .findFirst()
                .orElseThrow();

        // Only p1 should be counted, so metricValue = 1
        assertThat(mostSolved.getMetricValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Every metric is 0 -> no award rows")
    void everyMetricIsZero_noAwardRows() {
        LocalDate weekStart = LocalDate.of(2026, 4, 6);
        createUser("Alice", "alice@dau.ac.in", Role.ROLE_USER);
        entityManager.flush();

        weeklyAwardService.computeWeek(weekStart, null);

        List<WeeklyAwardDto> awards = weeklyAwardService.getAwardsByWeek(weekStart);
        assertThat(awards).isEmpty();
    }

    @Test
    @DisplayName("Two members tied on most solved -> both awarded")
    void twoMembersTiedOnMostSolved_bothAwarded() {
        LocalDate weekStart = LocalDate.of(2026, 4, 6);
        User u1 = createUser("Alice", "alice@dau.ac.in", Role.ROLE_USER);
        User u2 = createUser("Bob", "bob@dau.ac.in", Role.ROLE_USER);

        CfProblem p1 = createProblem(1001, "A");
        CfProblem p2 = createProblem(1001, "B");

        LocalDateTime insideWeekUtc = LocalDateTime.ofInstant(
                Instant.parse("2026-04-08T10:00:00Z"), ZoneId.of("UTC"));
        createSolve(u1, p1, insideWeekUtc);
        createSolve(u2, p2, insideWeekUtc);
        entityManager.flush();

        weeklyAwardService.computeWeek(weekStart, null);

        List<WeeklyAwardDto> awards = weeklyAwardService.getAwardsByWeek(weekStart);
        List<WeeklyAwardDto> mostSolved = awards.stream()
                .filter(a -> a.getAwardType() == AwardType.MOST_SOLVED)
                .toList();

        assertThat(mostSolved).hasSize(2);
        assertThat(mostSolved.stream().map(WeeklyAwardDto::getUserId)).containsExactlyInAnyOrder(u1.getId(), u2.getId());
        assertThat(mostSolved.get(0).getMetricValue()).isEqualTo(1);
    }

    @Test
    @DisplayName("Scheduled run executes twice -> no duplicate rows")
    void scheduledRunExecutesTwice_noDuplicateRows() {
        LocalDate weekStart = LocalDate.of(2026, 4, 6);
        User u = createUser("Alice", "alice@dau.ac.in", Role.ROLE_USER);
        CfProblem p = createProblem(1001, "A");
        LocalDateTime insideWeekUtc = LocalDateTime.ofInstant(
                Instant.parse("2026-04-08T10:00:00Z"), ZoneId.of("UTC"));
        createSolve(u, p, insideWeekUtc);
        entityManager.flush();

        // Run 1
        weeklyAwardService.computeWeek(weekStart, null);
        int count1 = weeklyAwardService.getAwardsByWeek(weekStart).size();
        assertThat(count1).isGreaterThan(0);

        // Run 2 (without adminId -> skips existing awards)
        weeklyAwardService.computeWeek(weekStart, null);
        int count2 = weeklyAwardService.getAwardsByWeek(weekStart).size();
        assertThat(count2).isEqualTo(count1);
    }

    @Test
    @DisplayName("Admin recompute -> rows replaced and computed_by set")
    void adminRecompute_rowsReplaced_computedBySet() {
        LocalDate weekStart = LocalDate.of(2026, 4, 6);
        User admin = createUser("Admin", "admin@dau.ac.in", Role.ROLE_ADMIN);
        User u = createUser("Alice", "alice@dau.ac.in", Role.ROLE_USER);
        CfProblem p = createProblem(1001, "A");
        LocalDateTime insideWeekUtc = LocalDateTime.ofInstant(
                Instant.parse("2026-04-08T10:00:00Z"), ZoneId.of("UTC"));
        createSolve(u, p, insideWeekUtc);
        entityManager.flush();

        // Initial scheduler computation: computed_by is null
        weeklyAwardService.computeWeek(weekStart, null);
        WeeklyAward initial = weeklyAwardRepository.findByWeekStartOrderByAwardTypeAscUserIdAsc(weekStart).get(0);
        assertThat(initial.getComputedBy()).isNull();

        // Admin recompute: computed_by is admin
        weeklyAwardService.computeWeek(weekStart, admin.getId());
        WeeklyAward recomputed = weeklyAwardRepository.findByWeekStartOrderByAwardTypeAscUserIdAsc(weekStart).get(0);
        assertThat(recomputed.getComputedBy()).isNotNull();
        assertThat(recomputed.getComputedBy().getId()).isEqualTo(admin.getId());
    }
}
