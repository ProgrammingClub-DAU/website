package com.cpclub.backend.security;

import com.cpclub.backend.score.entity.AwardType;
import com.cpclub.backend.score.entity.WeeklyAward;
import com.cpclub.backend.score.repository.WeeklyAwardRepository;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Public reads that returned 500 in production, run against real PostgreSQL through
 * the real filter chain.
 *
 * <p>Both halves matter. H2 accepted the null search parameter that PostgreSQL
 * rejects, and a test inside a transaction keeps the Hibernate session open, which
 * hides a lazy load outside one. So this class runs on PostgreSQL and is
 * deliberately not {@code @Transactional}: each request gets only the transactions
 * production would give it.</p>
 */
class PublicEndpointsPostgresTest extends PostgresIntegrationTest {

    /** A Monday, as every award week starts. */
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 9);

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WeeklyAwardRepository weeklyAwardRepository;

    private MockMvc mockMvc;
    private User winner;
    private WeeklyAward award;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        winner = userRepository.save(User.builder()
                .name("Weekly Winner")
                .email("public-endpoints-winner@dau.ac.in")
                .role(Role.ROLE_USER)
                .codeforcesHandle("weekly_winner")
                .build());
        award = weeklyAwardRepository.save(WeeklyAward.builder()
                .weekStart(WEEK)
                .awardType(AwardType.CODER_OF_THE_WEEK)
                .user(winner)
                .metricValue(42)
                .computedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());
    }

    @AfterEach
    void tearDown() {
        weeklyAwardRepository.deleteById(award.getId());
        userRepository.deleteById(winner.getId());
    }

    @Test
    @DisplayName("The latest weekly awards name their winners (the user is loaded lazily)")
    void latestWeeklyAwards() throws Exception {
        mockMvc.perform(get("/api/awards/weekly/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.computed").value(true))
                .andExpect(jsonPath("$.data.awards[0].userName").value("Weekly Winner"));
    }

    @Test
    @DisplayName("One week's awards, and a member's award history, name the winner too")
    void awardsByWeekAndByMember() throws Exception {
        mockMvc.perform(get("/api/awards/weekly/" + WEEK))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userName").value("Weekly Winner"));

        mockMvc.perform(get("/api/awards/users/" + winner.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].userName").value("Weekly Winner"));
    }

    @Test
    @DisplayName("The members directory works with no search text")
    void membersDirectoryWithoutQuery() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        mockMvc.perform(get("/api/users").param("query", "weekly"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("Weekly Winner"));
    }

    @Test
    @DisplayName("The website credits are readable signed out, like the rest of the members page")
    void platformCreatorsArePublic() throws Exception {
        mockMvc.perform(get("/api/users/platform-creators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("A bad request is a 4xx, not a server error")
    void clientErrorsAreNotServerErrors() throws Exception {
        // A path nothing serves, under a public prefix so security lets it through.
        mockMvc.perform(get("/api/awards/no-such-thing/at-all"))
                .andExpect(status().isNotFound());

        // A malformed date in the path.
        mockMvc.perform(get("/api/awards/weekly/not-a-date"))
                .andExpect(status().isBadRequest());

        // A page size outside @Min/@Max.
        mockMvc.perform(get("/api/users").param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}
