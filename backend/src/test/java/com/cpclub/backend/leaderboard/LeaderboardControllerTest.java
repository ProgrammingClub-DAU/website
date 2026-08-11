package com.cpclub.backend.leaderboard;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.controller.LeaderboardController;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaderboardControllerTest {

    private MockMvc mockMvc;
    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        leaderboardService = mock(LeaderboardService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaderboardController(leaderboardService)).build();
    }

    @Test
    void getLeaderboard_returnsPaginatedRankings() throws Exception {
        LeaderboardResponseDto member = new LeaderboardResponseDto(21, 1L, "Alice", "alice_cf", 1900, "Candidate Master");
        when(leaderboardService.getLeaderboard(1, 20))
                .thenReturn(new PagedResponse<>(List.of(member), 1, 20, 21, 2, true));

        mockMvc.perform(get("/api/leaderboard").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.content[0].rank").value(21))
                .andExpect(jsonPath("$.data.content[0].tier").value("Candidate Master"));
    }
}
