package com.cpclub.backend.stats.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
 * Provides content and pageNumber/pageSize Jackson property aliases for compatibility
 * with standard Spring PagedResponse consumers.
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
    private java.time.LocalDateTime computedAt;
    private List<StatsLeaderboardEntryDto> entries;

    @JsonProperty("content")
    public List<StatsLeaderboardEntryDto> getContent() {
        return entries;
    }

    @JsonProperty("content")
    public void setContent(List<StatsLeaderboardEntryDto> content) {
        this.entries = content;
    }

    @JsonProperty("pageNumber")
    public int getPageNumber() {
        return page;
    }

    @JsonProperty("pageSize")
    public int getPageSize() {
        return size;
    }
}
