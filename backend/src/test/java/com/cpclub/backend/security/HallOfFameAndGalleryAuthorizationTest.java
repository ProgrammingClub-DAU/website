package com.cpclub.backend.security;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may read and write the Hall of Fame and the gallery.
 *
 * <p>Runs through the real filter chain. The gallery rule is the one worth
 * pinning: {@code /api/gallery/**} is admin-only, so the public
 * {@code /api/gallery/photos} works only because it is matched first. Reordering
 * those two lines would make the gallery page demand a login, and nothing else
 * would notice.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class HallOfFameAndGalleryAuthorizationTest {

    private static final String ENTRY = """
            {"heading":"ICPC Regional","achievedOn":"2026-01-10"}
            """;

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
    @DisplayName("Anyone can read the Hall of Fame")
    void hallOfFameIsPublic() throws Exception {
        mockMvc.perform(get("/api/hall-of-fame")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anyone can read the gallery, despite the admin rule on /api/gallery/**")
    void galleryIsPublic() throws Exception {
        mockMvc.perform(get("/api/gallery/photos")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("A signed-out caller cannot create an entry")
    void anonymousCannotCreate() throws Exception {
        mockMvc.perform(post("/api/hall-of-fame")
                        .contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("An ordinary member cannot create, edit or delete an entry")
    void membersCannotWrite() throws Exception {
        mockMvc.perform(post("/api/hall-of-fame")
                        .contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/hall-of-fame/1")
                        .contentType(MediaType.APPLICATION_JSON).content(ENTRY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/hall-of-fame/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("An ordinary member still cannot reach the admin gallery endpoints")
    void adminGalleryStillProtected() throws Exception {
        mockMvc.perform(post("/api/gallery/members")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
