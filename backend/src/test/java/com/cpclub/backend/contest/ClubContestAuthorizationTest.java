package com.cpclub.backend.contest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may call the club contest and season endpoints.
 *
 * <p>The interesting case is {@code /upsolve/me}: it is a GET under
 * {@code /api/club-contests/**}, which the URL rules make public, so the method-level
 * {@code @PreAuthorize} is the only thing requiring a login. If it is ever dropped,
 * {@link #anonymousMyUpsolves_is401} fails.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ClubContestAuthorizationTest {

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
    @DisplayName("USER POST /api/club-contests is 403")
    void userCreatesContest_is403() throws Exception {
        mockMvc.perform(post("/api/club-contests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cfContestId\":102345,\"kind\":\"MASHUP\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER cannot force a sync, delete a contest, or manage seasons")
    void userAdminWrites_are403() throws Exception {
        mockMvc.perform(post("/api/club-contests/1/sync")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/club-contests/1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/seasons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"S\",\"startsOn\":\"2026-01-01\",\"endsOn\":\"2026-06-30\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/seasons/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous GET /api/club-contests/1 is not 401")
    void anonymousRanklist_isPublic() throws Exception {
        // 404 here: no such contest. The point is that it is not 401.
        mockMvc.perform(get("/api/club-contests/1")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/api/club-contests")).andExpect(status().isOk());
        mockMvc.perform(get("/api/club-contests/1/upsolve")).andExpect(status().is(not(401)));
        mockMvc.perform(get("/api/seasons")).andExpect(status().isOk());
        mockMvc.perform(get("/api/seasons/1/championship")).andExpect(status().is(not(401)));
    }

    @Test
    @DisplayName("Anonymous GET /api/club-contests/upsolve/me is 401")
    void anonymousMyUpsolves_is401() throws Exception {
        mockMvc.perform(get("/api/club-contests/upsolve/me")).andExpect(status().isUnauthorized());
    }
}
