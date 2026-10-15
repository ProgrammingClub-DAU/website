package com.cpclub.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies Phase 3 URL security ordering rules defined in Playbook Section 4.12 & Table 4.13.
 */
@SpringBootTest
@ActiveProfiles("test")
class Phase3SecurityOrderingTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER POST /api/events/1/registration is not 403 (proves ADMIN wildcard did not swallow it)")
    void userPostEventRegistration_isNotForbidden() throws Exception {
        // Until Stage 5B builds the controller endpoint, this returns 404/405, NOT 403
        mockMvc.perform(post("/api/events/1/registration"))
                .andExpect(status().is(not(403)));
    }

    @Test
    @DisplayName("Anonymous GET /api/events/1/ics is not 401 (public ICS endpoint)")
    void anonymousGetEventIcs_isNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/events/1/ics"))
                .andExpect(status().is(not(401)));
    }

    @Test
    @DisplayName("Anonymous GET /api/blogs/mine returns 401 Unauthorized")
    void anonymousGetBlogsMine_returns401() throws Exception {
        mockMvc.perform(get("/api/blogs/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER GET /api/blogs/review-queue returns 403 Forbidden")
    void userGetBlogsReviewQueue_returns403() throws Exception {
        mockMvc.perform(get("/api/blogs/review-queue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous GET /api/stats/users/1 is not 401 (public stats)")
    void anonymousGetStatsUser_isNotUnauthorized() throws Exception {
        mockMvc.perform(get("/api/stats/users/1"))
                .andExpect(status().is(not(401)));
    }
}
