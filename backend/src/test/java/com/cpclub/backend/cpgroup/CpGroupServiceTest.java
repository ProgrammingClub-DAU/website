package com.cpclub.backend.cpgroup;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberDetailDto;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberRowDto;
import com.cpclub.backend.cpgroup.dto.CpGroupRequests;
import com.cpclub.backend.cpgroup.service.CpGroupService;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.stats.entity.PlatformDailyTotal;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
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
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The CP Practice Group: membership, and the activity numbers admins see. */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CpGroupServiceTest {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Autowired private CpGroupService service;
    @Autowired private EntityManager entityManager;

    private final AtomicInteger nextContest = new AtomicInteger(7000);
    private final LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
    private final LocalDate todayIst = LocalDate.now(IST);

    private User admin;
    private User asha;
    private User bilal;

    @BeforeEach
    void setUp() {
        admin = user("Admin Person", "cp-admin@dau.ac.in", null, null, Role.ROLE_ADMIN);
        asha = user("Asha", "cp-asha@dau.ac.in", "asha_cf", "asha_lc", Role.ROLE_USER);
        asha.setRating(1350);
        asha.setLeetcodeRating(1600);
        asha.setLeetcodeTotalSolved(150);
        bilal = user("Bilal", "cp-bilal@dau.ac.in", null, null, Role.ROLE_USER);
        entityManager.flush();
    }

    private User user(String name, String email, String cf, String lc, Role role) {
        User u = User.builder().name(name).email(email).role(role)
                .codeforcesHandle(cf).leetcodeHandle(lc).batchYear(2028)
                .emailVerifiedAt(LocalDateTime.now()).build();
        entityManager.persist(u);
        return u;
    }

    private void solve(User user, boolean gym, LocalDateTime at) {
        int contestId = gym ? 100000 + nextContest.incrementAndGet() : nextContest.incrementAndGet();
        CfProblem p = CfProblem.builder().problemKey(contestId + "/A").contestId(contestId).problemIndex("A")
                .name("Problem " + contestId).rating(1200).solvedCount(10).build();
        entityManager.persist(p);
        entityManager.persist(CfSolve.builder().user(user).problem(p).firstAcAt(at)
                .firstAcSubmissionId(System.nanoTime()).participantType("PRACTICE").build());
    }

    private void leetcodeTotal(User user, int daysAgo, int total) {
        entityManager.persist(PlatformDailyTotal.builder().user(user).platform("LEETCODE")
                .capturedOn(todayIst.minusDays(daysAgo)).totalSolved(total).build());
    }

    private void contest(User user, Platform platform, int daysAgo, Integer oldRating, Integer newRating) {
        entityManager.persist(ContestParticipation.builder().user(user).platform(platform)
                .externalContestId(String.valueOf(nextContest.incrementAndGet()))
                .contestName(platform + " Round " + daysAgo).startedAt(nowUtc.minusDays(daysAgo))
                .rated(oldRating != null).contestRank(500).oldRating(oldRating).newRating(newRating).build());
    }

    private CpGroupMemberRowDto row(User user) {
        return service.listMembers().stream().filter(r -> r.userId().equals(user.getId())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Adding puts students in a batch; adding again moves them")
    void addAndMove() {
        CpGroupRequests.AddResult first = service.addMembers(
                new CpGroupRequests.AddMembers(List.of(asha.getId(), bilal.getId()), 2029), admin.getId());
        assertThat(first.added()).isEqualTo(2);

        CpGroupRequests.AddResult again = service.addMembers(
                new CpGroupRequests.AddMembers(List.of(bilal.getId(), 999_999L), 2030), admin.getId());
        assertThat(again.added()).isZero();
        assertThat(again.moved()).isEqualTo(1);
        entityManager.flush();

        assertThat(service.listMembers()).extracting(CpGroupMemberRowDto::name, CpGroupMemberRowDto::batchYear)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Asha", 2029),
                        org.assertj.core.groups.Tuple.tuple("Bilal", 2030));
        assertThat(row(asha).profileBatchYear()).isEqualTo(2028);
    }

    @Test
    @DisplayName("Codeforces solves over 7 days, 30 days and overall, without gym")
    void codeforcesWindows() {
        service.addMembers(new CpGroupRequests.AddMembers(List.of(asha.getId()), 2029), admin.getId());
        solve(asha, false, nowUtc.minusDays(1));
        solve(asha, false, nowUtc.minusDays(3));
        solve(asha, false, nowUtc.minusDays(20));
        solve(asha, false, nowUtc.minusDays(100));
        solve(asha, true, nowUtc.minusDays(2));
        entityManager.flush();

        CpGroupMemberRowDto r = row(asha);
        assertThat(r.cfSolved7d()).isEqualTo(2);
        assertThat(r.cfSolved30d()).isEqualTo(3);
        assertThat(r.cfSolvedAll()).isEqualTo(4);
        assertThat(r.cfRating()).isEqualTo(1350);
    }

    @Test
    @DisplayName("LeetCode solves are the rise since each window's snapshot")
    void leetcodeWindows() {
        service.addMembers(new CpGroupRequests.AddMembers(List.of(asha.getId(), bilal.getId()), 2029), admin.getId());
        leetcodeTotal(asha, 40, 100);
        leetcodeTotal(asha, 30, 110);
        leetcodeTotal(asha, 7, 140);
        leetcodeTotal(asha, 1, 145);
        leetcodeTotal(asha, 0, 150);
        entityManager.flush();

        CpGroupMemberRowDto r = row(asha);
        assertThat(r.lcSolved7d()).isEqualTo(10);
        assertThat(r.lcSolved30d()).isEqualTo(40);
        assertThat(r.lcSolvedAll()).isEqualTo(150);
        assertThat(r.lastActiveOn()).isEqualTo(todayIst);

        // No LeetCode at all: no numbers, not zeros.
        CpGroupMemberRowDto none = row(bilal);
        assertThat(none.lcSolved7d()).isNull();
        assertThat(none.lcSolvedAll()).isNull();
        assertThat(none.lastActiveOn()).isNull();
    }

    @Test
    @DisplayName("Contests and rating change over the last 30 days, per platform")
    void contests() {
        service.addMembers(new CpGroupRequests.AddMembers(List.of(asha.getId()), 2029), admin.getId());
        contest(asha, Platform.CODEFORCES, 10, 1200, 1250);
        contest(asha, Platform.CODEFORCES, 4, 1250, 1290);
        contest(asha, Platform.LEETCODE, 5, 1500, 1480);
        contest(asha, Platform.CODEFORCES, 40, 1100, 1200);
        contest(asha, Platform.CODEFORCES, 12, null, null);
        entityManager.flush();

        CpGroupMemberRowDto r = row(asha);
        assertThat(r.contests30d()).isEqualTo(4);
        assertThat(r.cfRatingChange30d()).isEqualTo(90);
        assertThat(r.lcRatingChange30d()).isEqualTo(-20);
        assertThat(r.lastActiveOn()).isEqualTo(nowUtc.minusDays(4).atOffset(ZoneOffset.UTC).atZoneSameInstant(IST).toLocalDate());
    }

    @Test
    @DisplayName("A student's page: 30 days of solves, contests, recent solves, notes and who added them")
    void detail() {
        service.addMembers(new CpGroupRequests.AddMembers(List.of(asha.getId()), 2029), admin.getId());
        solve(asha, false, nowUtc.minusDays(1));
        solve(asha, false, nowUtc.minusDays(20));
        solve(asha, true, nowUtc.minusHours(2));
        leetcodeTotal(asha, 1, 145);
        leetcodeTotal(asha, 0, 150);
        contest(asha, Platform.CODEFORCES, 10, 1200, 1250);
        service.updateMember(asha.getId(), new CpGroupRequests.UpdateMember(null, "  Needs DP practice.  "), admin.getId());
        entityManager.flush();

        CpGroupMemberDetailDto d = service.memberDetail(asha.getId());

        assertThat(d.daily()).hasSize(30);
        assertThat(d.daily().get(0).date()).isEqualTo(todayIst.minusDays(29));
        assertThat(d.daily().stream().mapToInt(com.cpclub.backend.stats.dto.MemberActivityDto.DailySolves::codeforces).sum()).isEqualTo(2);
        assertThat(d.daily().get(29).leetcode()).isEqualTo(5);
        assertThat(d.daily().get(28).leetcode()).isNull();
        assertThat(d.contests()).hasSize(1);
        assertThat(d.contests().get(0).newRating()).isEqualTo(1250);
        assertThat(d.recentSolves()).hasSize(3);
        assertThat(d.recentSolves().get(0).url()).contains("/gym/");
        assertThat(d.notes()).isEqualTo("Needs DP practice.");
        assertThat(d.notesUpdatedBy()).isEqualTo("Admin Person");
        assertThat(d.addedBy()).isEqualTo("Admin Person");
        assertThat(d.summary().hasNotes()).isTrue();
    }

    @Test
    @DisplayName("Notes are cleared with an empty string; batch changes alone keep them")
    void notesAndBatch() {
        service.addMembers(new CpGroupRequests.AddMembers(List.of(asha.getId()), 2029), admin.getId());
        service.updateMember(asha.getId(), new CpGroupRequests.UpdateMember(null, "Talked on 12 Nov"), admin.getId());
        service.updateMember(asha.getId(), new CpGroupRequests.UpdateMember(2030, null), admin.getId());
        entityManager.flush();
        assertThat(row(asha).batchYear()).isEqualTo(2030);
        assertThat(row(asha).hasNotes()).isTrue();

        service.updateMember(asha.getId(), new CpGroupRequests.UpdateMember(null, "   "), admin.getId());
        entityManager.flush();
        assertThat(service.memberDetail(asha.getId()).notes()).isNull();
    }

    @Test
    @DisplayName("A student sees only whether they are a member; removing takes them out")
    void membershipAndRemoval() {
        service.addMembers(new CpGroupRequests.AddMembers(List.of(asha.getId()), 2029), admin.getId());
        entityManager.flush();

        CpGroupRequests.MyMembership mine = service.myMembership(asha.getId());
        assertThat(mine.member()).isTrue();
        assertThat(mine.batchYear()).isEqualTo(2029);
        assertThat(service.myMembership(bilal.getId()).member()).isFalse();

        service.removeMember(asha.getId());
        entityManager.flush();
        assertThat(service.myMembership(asha.getId()).member()).isFalse();
        assertThat(service.listMembers()).isEmpty();
        assertThatThrownBy(() -> service.memberDetail(asha.getId())).isInstanceOf(ResourceNotFoundException.class);
    }
}
