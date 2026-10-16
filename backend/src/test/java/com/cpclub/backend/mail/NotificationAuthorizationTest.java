package com.cpclub.backend.mail;

import com.cpclub.backend.mail.service.UnsubscribeTokenService;
import com.cpclub.backend.mail.service.UnsubscribeTokenService.Kind;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who may reach the unsubscribe and RSVP endpoints. */
@SpringBootTest(properties = "cpclub.mail.unsubscribe-secret=0123456789abcdef0123456789abcdef")
@ActiveProfiles("test")
@Transactional
class NotificationAuthorizationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private UnsubscribeTokenService tokens;
    @Autowired private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Anonymous POST unsubscribe with a valid token is 200, and turns the preference off")
    void anonymousOneClickUnsubscribe() throws Exception {
        User user = userRepository.save(User.builder().name("Member").email("member-unsub@dau.ac.in")
                .notifyContests(true).build());

        // Exactly what a mail client sends for RFC 8058 one-click.
        mockMvc.perform(post("/api/notifications/unsubscribe")
                        .param("token", tokens.issue(user.getId(), Kind.CONTESTS))
                        .contentType("application/x-www-form-urlencoded")
                        .content("List-Unsubscribe=One-Click"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("CONTESTS"));

        assertFalse(userRepository.findById(user.getId()).orElseThrow().isNotifyContests());
    }

    @Test
    @DisplayName("Anonymous GET unsubscribe works too, for the link in the email body")
    void anonymousGetUnsubscribe() throws Exception {
        User user = userRepository.save(User.builder().name("Member").email("member-unsub2@dau.ac.in")
                .notifyEvents(true).build());

        mockMvc.perform(get("/api/notifications/unsubscribe").param("token", tokens.issue(user.getId(), Kind.EVENTS)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.kind").value("EVENTS"));
    }

    @Test
    @DisplayName("A forged token is a 400, not a 401: the endpoint is public, the token is the credential")
    void forgedToken_is400() throws Exception {
        mockMvc.perform(post("/api/notifications/unsubscribe").param("token", "Zm9yZ2Vk"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Anonymous callers cannot RSVP")
    void anonymousRsvp_is401() throws Exception {
        mockMvc.perform(post("/api/events/1/registration")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/events/1/registration")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Members cannot read the RSVP list or mark attendance")
    void memberAdminRsvpRoutes_are403() throws Exception {
        mockMvc.perform(get("/api/events/1/registrations")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/events/1/registrations/mark-attended")
                        .contentType("application/json").content("{\"userIds\":[1]}"))
                .andExpect(status().isForbidden());
    }
}
