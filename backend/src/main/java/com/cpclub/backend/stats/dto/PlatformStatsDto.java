package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Statistics block for a single linked competitive programming platform (Section 1.1, 4.9).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsDto {
    private StatsPlatform platform;
    private String handle;
    private Integer rating;
    private Integer maxRating;
    private Integer solved;
    private Integer contests;
    private Double avgProblemRating;
    private Integer ratedSolvesCount;
    private LeetCodeDifficultyDto lcDifficulty;
    private LocalDate trackingSince;
    private LocalDateTime lastSyncedAt;
}
