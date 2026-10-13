package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solved breakdown across LeetCode difficulty tiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeetCodeDifficultyDto {
    private Integer easy;
    private Integer medium;
    private Integer hard;
}
