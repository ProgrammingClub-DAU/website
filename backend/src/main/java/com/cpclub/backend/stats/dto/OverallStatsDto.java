package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Aggregated cross-platform statistics block (Codeforces + LeetCode).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverallStatsDto {
    private Integer solved;
    private boolean solvedPartial;
    private Integer contests;
}
