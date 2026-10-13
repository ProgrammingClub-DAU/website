package com.cpclub.backend.stats.dto;

import com.cpclub.backend.user.entity.ClubRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entry on a statistics leaderboard.
 * Ranks are assigned with RANK() OVER (ORDER BY value DESC) so ties share a rank.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsLeaderboardEntryDto {
    private int rank;
    private Long userId;
    private String name;
    private String codeforcesHandle;
    private String leetcodeHandle;
    private String avatarUrl;
    private ClubRole clubRole;
    private Double value;
    private Integer basis;
}
