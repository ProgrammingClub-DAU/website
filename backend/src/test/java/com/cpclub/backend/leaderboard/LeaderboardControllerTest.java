package com.cpclub.backend.leaderboard;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.controller.LeaderboardController;
import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import com.cpclub.backend.leaderboard.dto.LeaderboardPlatform;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import com.cpclub.backend.stats.dto.*;
import com.cpclub.backend.user.entity.ClubRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LeaderboardControllerTest {

    private MockMvc mockMvc;
    private LeaderboardService leaderboardService;
    private com.cpclub.backend.stats.service.StatsService statsService;

    @BeforeEach
    void setUp() {
        leaderboardService = mock(LeaderboardService.class);
        statsService = mock(com.cpclub.backend.stats.service.StatsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new LeaderboardController(leaderboardService, statsService)).build();
    }

    @Test
    void getLeaderboard_returnsPaginatedRankings() throws Exception {
        LeaderboardResponseDto member = new LeaderboardResponseDto(21, 1L, "Alice", "alice_cf", 1900, "Candidate Master", "CORE",
                "https://lh3.googleusercontent.com/alice");
        when(leaderboardService.getLeaderboard(1, 20, LeaderboardPlatform.CODEFORCES, LeaderboardFilter.ALL))
                .thenReturn(new PagedResponse<>(List.of(member), 1, 20, 21, 2, true));

        mockMvc.perform(get("/api/leaderboard").param("page", "1").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.content[0].rank").value(21))
                .andExpect(jsonPath("$.data.content[0].tier").value("Candidate Master"));
    }

    @Test
    void getStatsLeaderboard_returnsBothContentAndEntries() throws Exception {
        StatsLeaderboardEntryDto entry = StatsLeaderboardEntryDto.builder()
                .rank(1)
                .userId(1L)
                .name("Madhav")
                .codeforcesHandle("madhavthesiya")
                .clubRole(ClubRole.CORE)
                .value(1611.0)
                .build();

        StatsLeaderboardResponseDto responseDto = StatsLeaderboardResponseDto.builder()
                .metric(StatsMetric.SOLVED)
                .platform(StatsPlatform.OVERALL)
                .period(StatsPeriod.ALL_TIME)
                .filter(LeaderboardFilter.ALL)
                .page(0)
                .size(50)
                .totalElements(1)
                .totalPages(1)
                .entries(List.of(entry))
                .build();

        when(statsService.getLeaderboard(eq(StatsMetric.SOLVED), any(), any(), eq(LeaderboardFilter.ALL), eq(0), eq(50)))
                .thenReturn(responseDto);

        mockMvc.perform(get("/api/leaderboard/stats").param("metric", "SOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.entries[0].name").value("Madhav"))
                .andExpect(jsonPath("$.data.content[0].name").value("Madhav"))
                .andExpect(jsonPath("$.data.entries[0].value").value(1611.0))
                .andExpect(jsonPath("$.data.content[0].value").value(1611.0))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.pageNumber").value(0))
                .andExpect(jsonPath("$.data.size").value(50))
                .andExpect(jsonPath("$.data.pageSize").value(50));
    }
}
