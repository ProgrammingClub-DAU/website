package com.cpclub.backend.security;

import com.cpclub.backend.security.jwt.JwtUtils;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/auth/refresh through the real filter chain.
 *
 * <p>{@code /api/auth/**} is permitAll in the filter chain, so the only thing that
 * keeps an anonymous caller from minting a token here is the method's
 * {@code @PreAuthorize}. The standalone controller test cannot see that; this does.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionRenewalIntegrationTest {

    private static final String EMAIL = "session-renewal-test@dau.ac.in";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private MockMvc mockMvc;
    private User member;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        member = userRepository.save(User.builder()
                .name("Session Renewal")
                .email(EMAIL)
                .role(Role.ROLE_USER)
                .build());
    }

    @Test
    @DisplayName("A valid token is exchanged for a fresh, valid token for the same member")
    void validTokenIsRenewed() throws Exception {
        String current = jwtUtils.generateTokenFromEmail(EMAIL, member.getId(), Role.ROLE_USER.name());

        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer " + current))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(EMAIL))
                .andReturn();

        String fresh = com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
        assertTrue(jwtUtils.validateJwtToken(fresh));
        assertEquals(EMAIL, jwtUtils.getUserNameFromJwtToken(fresh));
    }

    @Test
    @DisplayName("No token, no renewal")
    void anonymousCallerIsRefused() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A forged or garbled token is not renewed")
    void invalidTokenIsRefused() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer not.a.token"))
                .andExpect(status().isUnauthorized());
    }
}
