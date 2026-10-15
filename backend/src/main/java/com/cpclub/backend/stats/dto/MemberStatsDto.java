package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full statistics summary for a single member over a chosen period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberStatsDto {
    private Long userId;
    private String name;
    private StatsPeriod period;
    private List<PlatformStatsDto> platforms;
    private OverallStatsDto overall;
    private MemberScoreDto overallScore;
}
