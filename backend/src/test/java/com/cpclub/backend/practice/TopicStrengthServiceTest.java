package com.cpclub.backend.practice;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfProblemTag;
import com.cpclub.backend.codeforces.entity.CfProblemTagId;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.practice.dto.TopicStrengthDto;
import com.cpclub.backend.practice.dto.TopicStrengthResponseDto;
import com.cpclub.backend.practice.service.TopicStrengthService;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class TopicStrengthServiceTest {

    @Autowired
    private TopicStrengthService topicStrengthService;

    @Autowired
    private EntityManager entityManager;

    private User createUser(String name, String email, String cfHandle) {
        User u = User.builder()
                .name(name)
                .email(email)
                .role(Role.ROLE_USER)
                .clubRole(ClubRole.STUDENT)
                .codeforcesHandle(cfHandle)
                .emailVerifiedAt(LocalDateTime.now())
                .build();
        entityManager.persist(u);
        return u;
    }

    private CfProblem createProblem(int contestId, String index, String... tags) {
        CfProblem p = CfProblem.builder()
                .problemKey(contestId + "/" + index)
                .contestId(contestId)
                .problemIndex(index)
                .name("Problem " + index)
                .rating(1400)
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
    @DisplayName("A problem tagged dp and greedy counts toward both")
    void problemTaggedDpAndGreedy_countsTowardBoth() {
        User user = createUser("Alice", "alice@dau.ac.in", "alice_cf");
        CfProblem problem = createProblem(1200, "A", "dp", "greedy");
        createSolve(user, problem);
        entityManager.flush();

        TopicStrengthResponseDto response = topicStrengthService.getUserTopicStrength(user.getId());

        assertThat(response.getReason()).isNull();
        assertThat(response.getTotalCountedSolves()).isEqualTo(1);

        TopicStrengthDto dpTopic = response.getTopics().stream()
                .filter(t -> t.getTag().equals("dp"))
                .findFirst().orElseThrow();
        TopicStrengthDto greedyTopic = response.getTopics().stream()
                .filter(t -> t.getTag().equals("greedy"))
                .findFirst().orElseThrow();

        assertThat(dpTopic.getMemberSolves()).isEqualTo(1);
        assertThat(greedyTopic.getMemberSolves()).isEqualTo(1);
    }

    @Test
    @DisplayName("Gym solve tagged dp is not counted (D3: contest_id >= 100000)")
    void gymSolveTaggedDp_notCounted() {
        User user = createUser("Alice", "alice@dau.ac.in", "alice_cf");
        CfProblem regularProblem = createProblem(1200, "A", "math");
        CfProblem gymProblem = createProblem(100500, "B", "dp"); // contestId >= 100000

        createSolve(user, regularProblem);
        createSolve(user, gymProblem);
        entityManager.flush();

        TopicStrengthResponseDto response = topicStrengthService.getUserTopicStrength(user.getId());

        assertThat(response.getTotalCountedSolves()).isEqualTo(1);

        TopicStrengthDto dpTopic = response.getTopics().stream()
                .filter(t -> t.getTag().equals("dp"))
                .findFirst().orElseThrow();
        TopicStrengthDto mathTopic = response.getTopics().stream()
                .filter(t -> t.getTag().equals("math"))
                .findFirst().orElseThrow();

        // Gym solve is ignored: dp count is 0
        assertThat(dpTopic.getMemberSolves()).isEqualTo(0);
        // Regular solve is counted: math count is 1
        assertThat(mathTopic.getMemberSolves()).isEqualTo(1);
    }

    @Test
    @DisplayName("Member with no Codeforces handle returns reason = NO_CODEFORCES_HANDLE and empty list")
    void memberWithNoCodeforcesHandle_returnsNoHandleReason() {
        User user = createUser("NoHandle", "nohandle@dau.ac.in", null);
        entityManager.flush();

        TopicStrengthResponseDto response = topicStrengthService.getUserTopicStrength(user.getId());

        assertThat(response.getReason()).isEqualTo("NO_CODEFORCES_HANDLE");
        assertThat(response.getTopics()).isEmpty();
    }
}
