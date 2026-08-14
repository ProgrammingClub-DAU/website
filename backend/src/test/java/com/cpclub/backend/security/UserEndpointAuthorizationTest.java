package com.cpclub.backend.security;

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
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies which {@code /api/users} reads an anonymous caller may perform.
 *
 * <p>This runs through the real filter chain. Every other MockMvc test in the
 * suite uses {@code standaloneSetup}, which builds a bare dispatcher with no
 * Spring Security in it — so those tests would pass even if authorization were
 * removed entirely. Nothing exercised the rules until this class.</p>
 *
 * <p>What is being protected: {@code /api/users/all} and {@code /api/users/profile}
 * return {@code UserResponseDto}, which carries the member's email address and
 * role. The public directory endpoints return {@code PublicUserResponseDto}, which
 * omits both. A regression that pointed a public endpoint at the wrong DTO, or
 * that widened the filter chain back to a blanket {@code GET /api/users/**}, would
 * publish every student's email — so both halves are asserted here.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class UserEndpointAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // The SecurityContext is a thread-local that outlives individual test classes,
        // and MockMvc's security integration reads whatever is in it. Any earlier test
        // that authenticated would otherwise make these requests non-anonymous and
        // quietly invert what they assert. Cleared here so the premise is stated rather
        // than inherited.
        SecurityContextHolder.clearContext();

        // Built from the application context with the security filter chain applied,
        // so requests pass through the same authorization rules production uses.
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Anonymous callers may read the members directory")
    void directoryIsPublic() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("The public directory never returns an email or a role")
    void directoryOmitsEmailAndRole() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..email").doesNotExist())
                .andExpect(jsonPath("$..role").doesNotExist());
    }

    @Test
    @DisplayName("Anonymous callers are refused the full user list, which carries emails")
    void fullUserListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous callers are refused the current-user profile")
    void ownProfileRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A public profile lookup by id is reachable without a token")
    void publicProfileByIdIsReachableAnonymously() throws Exception {
        // The id need not exist: a 404 still proves the request was authorized and
        // reached the controller, which is what this asserts. A 401 would not.
        mockMvc.perform(get("/api/users/999999"))
                .andExpect(status().isNotFound());
    }
}
