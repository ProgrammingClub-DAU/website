package com.cpclub.backend.score;

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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
public class AwardControllerTest {

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
    @DisplayName("Anonymous can call GET /api/awards/weekly/latest")
    void getLatestWeeklyAwards_publicAccess() throws Exception {
        mockMvc.perform(get("/api/awards/weekly/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Recompute current or future week returns 400")
    @WithMockUser(roles = "ADMIN")
    void recomputeCurrentWeek_returns400() throws Exception {
        LocalDate futureWeek = LocalDate.now().plusWeeks(1);

        mockMvc.perform(post("/api/admin/awards/weekly/" + futureWeek + "/recompute"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("USER role calling recompute returns 403")
    @WithMockUser(roles = "USER")
    void userRole_callingRecompute_returns403() throws Exception {
        LocalDate closedWeek = LocalDate.of(2026, 4, 6);

        mockMvc.perform(post("/api/admin/awards/weekly/" + closedWeek + "/recompute"))
                .andExpect(status().isForbidden());
    }
}
