package com.cpclub.backend.practice;

import com.cpclub.backend.codeforces.entity.CfAttempt;
import com.cpclub.backend.codeforces.entity.CfAttemptId;
import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfProblemTag;
import com.cpclub.backend.codeforces.entity.CfProblemTagId;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.practice.dto.PracticeSuggestionDto;
import com.cpclub.backend.practice.dto.PracticeSuggestionsResponseDto;
import com.cpclub.backend.practice.dto.TryAgainDto;
import com.cpclub.backend.practice.service.PracticeSuggestionService;
import com.cpclub.backend.user.entity.ClubRole;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Practice suggestions picked by skill: levels per topic, the 40-70% chance
 * window, the daily mix per member, and "try again" problems.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class PracticeSuggestionServiceTest {

    @Autowired
    private PracticeSuggestionService practiceSuggestionService;

    @Autowired
    private EntityManager entityManager;

    private final AtomicInteger nextContest = new AtomicInteger(5000);

    @BeforeEach
    void setUp() {
        practiceSuggestionService.clearCache();
    }

    private User createUser(String name, String email, String cfHandle, Integer rating) {
        User u = User.builder()
                .name(name)
                .email(email)
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.STUDENT)
                .codeforcesHandle(cfHandle)
                .rating(rating)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(u);
        return u;
    }

    private CfProblem createProblem(int rating, int solvedCount, String... tags) {
        int contestId = nextContest.incrementAndGet();
        CfProblem p = CfProblem.builder()
                .problemKey(contestId + "/A")
                .contestId(contestId)
                .problemIndex("A")
                .name("Problem " + contestId + "A")
                .rating(rating)
                .solvedCount(solvedCount)
                .build();
        entityManager.persist(p);
        for (String tag : tags) {
            CfProblemTag pt = new CfProblemTag(new CfProblemTagId(p.getId(), tag), p, tag);
            entityManager.persist(pt);
            p.getTags().add(pt);
        }
        return p;
    }

    private void createSolve(User user, CfProblem problem) {
        entityManager.persist(CfSolve.builder()
                .user(user)
                .problem(problem)
                .firstAcAt(LocalDateTime.now(ZoneId.of("UTC")))
                .firstAcSubmissionId(System.nanoTime())
                .participantType("PRACTICE")
                .build());
    }

    private void createAttempt(User user, CfProblem problem, String verdict, int daysAgo) {
        entityManager.persist(new CfAttempt(new CfAttemptId(user.getId(), problem.getId()), user, problem,
                LocalDateTime.now(ZoneId.of("UTC")).minusDays(daysAgo), System.nanoTime(), verdict));
    }

    /**
     * A member rated 1500 with 30 greedy solves at 1600 and no dp: overall level
     * 1550, dp level 1150 (an untouched topic, 400 below). Five dp problems at
     * 1500 make dp common near their level, so it is a focus topic.
     */
    private User greedyMember(String name) {
        User user = createUser(name, name.toLowerCase() + "@dau.ac.in", name.toLowerCase() + "_cf", 1500);
        for (int i = 0; i < 30; i++) createSolve(user, createProblem(1600, 100, "greedy"));
        return user;
    }

    @Test
    @DisplayName("A beginner with no solves gets 800-rated problems only")
    void beginner_getsEightHundredOnly() {
        User user = createUser("Beginner", "beginner@dau.ac.in", "beginner_cf", null);
        CfProblem easy = createProblem(800, 50, "implementation");
        CfProblem harder = createProblem(900, 60, "implementation");
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getReason()).isNull();
        assertThat(response.getOverallLevel()).isEqualTo(800);
        assertThat(response.getWeakTags()).isEmpty();
        // 900 at level 800 is a 36% chance: below the 40-70% window.
        assertThat(response.getSuggestions()).extracting(PracticeSuggestionDto::getProblemId)
                .contains(easy.getId())
                .doesNotContain(harder.getId());
        assertThat(response.getSuggestions()).allSatisfy(s -> {
            assertThat(s.getChance()).isBetween(40, 70);
            assertThat(s.getFocusTag()).isNull();
        });
    }

    @Test
    @DisplayName("Focus-topic problems are judged by the member's level in that topic")
    void weakTopic_pickedAtTopicLevel() {
        User user = greedyMember("Greedy");
        for (int i = 0; i < 5; i++) createProblem(1500, 400, "dp");
        CfProblem dpEasy = createProblem(1100, 300, "dp");
        CfProblem dpMid = createProblem(1200, 200, "dp");
        CfProblem dpTooHard = createProblem(1400, 500, "dp");
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getOverallLevel()).isEqualTo(1550);
        assertThat(response.getWeakTags()).first().isEqualTo("dp");
        List<PracticeSuggestionDto> dpPicks = response.getSuggestions().stream()
                .filter(s -> "dp".equals(s.getFocusTag())).toList();
        assertThat(dpPicks).extracting(PracticeSuggestionDto::getProblemId)
                .containsExactlyInAnyOrder(dpEasy.getId(), dpMid.getId())
                .doesNotContain(dpTooHard.getId());
        assertThat(dpPicks).allSatisfy(s -> assertThat(s.getFocusLevel()).isEqualTo(1150));
        assertThat(response.getSuggestions()).allSatisfy(s -> assertThat(s.getChance()).isBetween(40, 70));
    }

    @Test
    @DisplayName("Below 30 rated solves there are no focus topics")
    void fewSolves_noFocusTopics() {
        User user = createUser("Twelve", "twelve@dau.ac.in", "twelve_cf", 1500);
        for (int i = 0; i < 12; i++) createSolve(user, createProblem(1200, 100, "implementation"));
        CfProblem atLevel = createProblem(1500, 200, "graphs");
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getWeakTags()).isEmpty();
        assertThat(response.getSuggestions()).extracting(PracticeSuggestionDto::getProblemId).contains(atLevel.getId());
    }

    @Test
    @DisplayName("Two members with the same profile get different daily mixes")
    void sameProfile_differentMix() {
        User first = greedyMember("First");
        User second = greedyMember("Second");
        for (int i = 0; i < 5; i++) createProblem(1500, 400, "dp");
        for (int i = 0; i < 15; i++) createProblem(1100 + 100 * (i % 2), 300 - i, "dp");
        entityManager.flush();

        List<Long> firstIds = practiceSuggestionService.getSuggestions(first.getId()).getSuggestions().stream()
                .map(PracticeSuggestionDto::getProblemId).toList();
        List<Long> secondIds = practiceSuggestionService.getSuggestions(second.getId()).getSuggestions().stream()
                .map(PracticeSuggestionDto::getProblemId).toList();

        assertThat(firstIds).hasSize(10);
        assertThat(secondIds).hasSize(10);
        assertThat(firstIds).isNotEqualTo(secondIds);
    }

    @Test
    @DisplayName("Solved and skipped problems are not suggested")
    void solvedAndSkipped_notSuggested() {
        User user = createUser("Candidate", "candidate@dau.ac.in", "candidate_cf", null);
        CfProblem solved = createProblem(800, 300, "math");
        createSolve(user, solved);
        CfProblem skipped = createProblem(800, 250, "dp");
        CfProblem eligible = createProblem(800, 200, "greedy");
        entityManager.flush();

        practiceSuggestionService.skip(user.getId(), skipped.getId());
        entityManager.flush();

        assertThat(practiceSuggestionService.getSuggestions(user.getId()).getSuggestions())
                .extracting(PracticeSuggestionDto::getProblemId)
                .contains(eligible.getId())
                .doesNotContain(solved.getId(), skipped.getId());
    }

    @Test
    @DisplayName("The same day returns the cached list, less problems solved since")
    void cachedList_dropsProblemsSolvedSince() {
        User user = createUser("Cache", "cache@dau.ac.in", "cache_cf", null);
        CfProblem a = createProblem(800, 150, "strings");
        CfProblem b = createProblem(800, 140, "strings");
        entityManager.flush();

        PracticeSuggestionsResponseDto first = practiceSuggestionService.getSuggestions(user.getId());
        assertThat(practiceSuggestionService.getSuggestions(user.getId())).isSameAs(first);

        createSolve(user, a);
        entityManager.flush();

        assertThat(practiceSuggestionService.getSuggestions(user.getId()).getSuggestions())
                .extracting(PracticeSuggestionDto::getProblemId)
                .containsExactly(b.getId());
    }

    @Test
    @DisplayName("Try again lists failed problems not solved or skipped since, newest first")
    void tryAgain_listsUnsolvedFailures() {
        User user = createUser("Retry", "retry@dau.ac.in", "retry_cf", null);
        CfProblem older = createProblem(900, 100, "math");
        CfProblem newer = createProblem(800, 100, "greedy");
        CfProblem solvedLater = createProblem(800, 100, "dp");
        CfProblem skipped = createProblem(800, 100, "dp");
        createAttempt(user, older, "TIME_LIMIT_EXCEEDED", 9);
        createAttempt(user, newer, "WRONG_ANSWER", 2);
        createAttempt(user, solvedLater, "WRONG_ANSWER", 1);
        createSolve(user, solvedLater);
        createAttempt(user, skipped, "WRONG_ANSWER", 1);
        entityManager.flush();
        practiceSuggestionService.skip(user.getId(), skipped.getId());
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getTryAgain()).extracting(TryAgainDto::getProblemId)
                .containsExactly(newer.getId(), older.getId());
        TryAgainDto first = response.getTryAgain().get(0);
        assertThat(first.getLastVerdict()).isEqualTo("WRONG_ANSWER");
        assertThat(first.getTags()).containsExactly("greedy");
        assertThat(first.getChance()).isEqualTo(50);
        assertThat(first.getUrl()).isEqualTo("https://codeforces.com/problemset/problem/" + newer.getContestId() + "/A");
        // Shown under "try again", so not suggested a second time.
        assertThat(response.getSuggestions()).extracting(PracticeSuggestionDto::getProblemId)
                .doesNotContain(newer.getId());
    }

    @Test
    @DisplayName("Member with no Codeforces handle returns reason NO_CODEFORCES_HANDLE")
    void noCodeforcesHandle_returnsReasonNoHandle() {
        User user = createUser("No CF", "nocf@dau.ac.in", null, 1400);
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getReason()).isEqualTo("NO_CODEFORCES_HANDLE");
        assertThat(response.getRatingBandMin()).isNull();
        assertThat(response.getRatingBandMax()).isNull();
        assertThat(response.getOverallLevel()).isNull();
        assertThat(response.getWeakTags()).isEmpty();
        assertThat(response.getSuggestions()).isEmpty();
        assertThat(response.getTryAgain()).isEmpty();
    }
}
