package com.cpclub.backend.stats.dto;

import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Paginated response for a statistics leaderboard.
 * Includes trackingSince for LeetCode metrics so the UI can explain baseline dates.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsLeaderboardResponseDto {
    private StatsMetric metric;
    private StatsPlatform platform;
    private StatsPeriod period;
    private LeaderboardFilter filter;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private LocalDate trackingSince;
    private List<StatsLeaderboardEntryDto> entries;
}
