package com.cpclub.backend.blog;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may reach the blog and comment endpoints.
 *
 * <p>{@code GET /api/blogs/{id}/edit} sits under the public {@code GET /api/blogs/**}
 * rule, so only its {@code @PreAuthorize} makes it require a login -- and only the
 * 401 fix in GlobalExceptionHandler makes that refusal a 401 rather than a 403.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class BlogAuthorizationTest {

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
    @DisplayName("Anonymous POST /api/blogs is 401")
    void anonymousCreate_is401() throws Exception {
        mockMvc.perform(post("/api/blogs").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hello\",\"content\":\"World\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("USER POST /api/blogs/1/publish is 403")
    void memberPublish_is403() throws Exception {
        mockMvc.perform(post("/api/blogs/1/publish")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/blogs/1/reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"note\":\"no\"}")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/blogs/review-queue")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Anonymous GET /api/blogs/1/edit is 401")
    void anonymousEdit_is401() throws Exception {
        mockMvc.perform(get("/api/blogs/1/edit")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/blogs/mine")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Anonymous callers cannot write, edit or delete comments, but can read them")
    void anonymousComments() throws Exception {
        mockMvc.perform(post("/api/blogs/1/comments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"hi\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/comments/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"hi\"}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/comments/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/blogs/1")).andExpect(status().isUnauthorized());
        // 404 here -- no such post -- but not 401.
        mockMvc.perform(get("/api/blogs/1/comments")).andExpect(status().is(not(401)));
    }
}
