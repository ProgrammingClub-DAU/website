package com.cpclub.backend.practice;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfProblemTag;
import com.cpclub.backend.codeforces.entity.CfProblemTagId;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.practice.dto.PracticeSuggestionDto;
import com.cpclub.backend.practice.dto.PracticeSuggestionsResponseDto;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class PracticeSuggestionServiceTest {

    @Autowired
    private PracticeSuggestionService practiceSuggestionService;

    @Autowired
    private EntityManager entityManager;

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

    private CfProblem createProblem(int contestId, String index, int rating, int solvedCount, String... tags) {
        CfProblem p = CfProblem.builder()
                .problemKey(contestId + "/" + index)
                .contestId(contestId)
                .problemIndex(index)
                .name("Problem " + contestId + index)
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

    @Test
    @DisplayName("Unrated member gets target band 800 to 1000")
    void unratedMember_targetBand800To1000() {
        User user = createUser("Unrated User", "unrated@dau.ac.in", "unrated_cf", null);
        createProblem(1001, "A", 900, 50, "implementation");
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getReason()).isNull();
        assertThat(response.getRatingBandMin()).isEqualTo(800);
        assertThat(response.getRatingBandMax()).isEqualTo(1000);
        assertThat(response.getSuggestions()).isNotEmpty();
    }

    @Test
    @DisplayName("Member with 12 counted solves uses all tags without weak-tag filter")
    void memberWith12Solves_everyTagUsedNoWeakTagFilter() {
        User user = createUser("Twelve Solves", "twelve@dau.ac.in", "twelve_cf", 1500);

        // Solve 12 problems (< 30 threshold)
        for (int i = 1; i <= 12; i++) {
            CfProblem p = createProblem(2000 + i, "A", 1200, 100, "implementation");
            createSolve(user, p);
        }

        // Target band for 1500 is 1600 - 1800
        CfProblem inBandProblem = createProblem(1500, "C", 1600, 200, "graphs");
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(response.getReason()).isNull();
        assertThat(response.getRatingBandMin()).isEqualTo(1600);
        assertThat(response.getRatingBandMax()).isEqualTo(1800);
        assertThat(response.getWeakTags()).isEmpty();
        assertThat(response.getSuggestions()).extracting(PracticeSuggestionDto::getProblemKey)
                .contains(inBandProblem.getProblemKey());
    }

    @Test
    @DisplayName("Solved and skipped problems in the band are neither suggested")
    void solvedAndSkippedProblemsInBand_neitherSuggested() {
        User user = createUser("Candidate User", "candidate@dau.ac.in", "candidate_cf", 1200);

        // Target band for 1200 is 1300 - 1500
        CfProblem solved = createProblem(1101, "B", 1400, 300, "math");
        createSolve(user, solved);

        CfProblem skipped = createProblem(1102, "B", 1400, 250, "dp");

        CfProblem eligible = createProblem(1103, "B", 1400, 200, "greedy");

        entityManager.flush();

        // Skip the second problem
        practiceSuggestionService.skip(user.getId(), skipped.getId());
        entityManager.flush();

        PracticeSuggestionsResponseDto response = practiceSuggestionService.getSuggestions(user.getId());

        List<Long> suggestedIds = response.getSuggestions().stream()
                .map(PracticeSuggestionDto::getProblemId)
                .toList();

        assertThat(suggestedIds).contains(eligible.getId());
        assertThat(suggestedIds).doesNotContain(solved.getId());
        assertThat(suggestedIds).doesNotContain(skipped.getId());
    }

    @Test
    @DisplayName("Called twice on the same IST date returns identical cached list")
    void calledTwiceOnSameIstDate_identicalListCached() {
        User user = createUser("Cache User", "cache@dau.ac.in", "cache_cf", 1300);
        createProblem(1201, "A", 1400, 150, "strings");
        entityManager.flush();

        PracticeSuggestionsResponseDto first = practiceSuggestionService.getSuggestions(user.getId());
        PracticeSuggestionsResponseDto second = practiceSuggestionService.getSuggestions(user.getId());

        assertThat(second).isSameAs(first);
        assertThat(second.getSuggestions()).isEqualTo(first.getSuggestions());
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
        assertThat(response.getWeakTags()).isEmpty();
        assertThat(response.getSuggestions()).isEmpty();
    }
}
