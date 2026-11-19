package com.cpclub.backend.stats;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.user.entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** /api/stats/users/{id}/activity: public, like the rest of a member's stats. */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberActivityEndpointTest {

    @Autowired private WebApplicationContext context;
    @Autowired private EntityManager entityManager;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Signed out: 30 days of solves and the latest solves")
    void anonymous_getsActivity() throws Exception {
        User member = User.builder().name("Activity Member").email("activity@dau.ac.in")
                .codeforcesHandle("activity_cf").build();
        entityManager.persist(member);
        CfProblem problem = CfProblem.builder().problemKey("77001/B").contestId(77001).problemIndex("B")
                .name("Two Sums").rating(1400).solvedCount(10).build();
        entityManager.persist(problem);
        entityManager.persist(CfSolve.builder().user(member).problem(problem)
                .firstAcAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(3))
                .firstAcSubmissionId(1L).participantType("PRACTICE").build());
        entityManager.flush();

        mockMvc.perform(get("/api/stats/users/" + member.getId() + "/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.daily", hasSize(30)))
                .andExpect(jsonPath("$.data.recentSolves", hasSize(1)))
                .andExpect(jsonPath("$.data.recentSolves[0].name", is("Two Sums")))
                .andExpect(jsonPath("$.data.recentSolves[0].url", is("https://codeforces.com/problemset/problem/77001/B")));
    }

    @Test
    @DisplayName("Unknown member: 404, not 401")
    void unknownMember_is404() throws Exception {
        mockMvc.perform(get("/api/stats/users/987654321/activity")).andExpect(status().isNotFound());
    }
}
