package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Breakdown of a member's Overall Aggregated Score and its three components (Section 1.2, Stage 3B).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberScoreDto {
    private int overallScore;
    private int ratingScore;
    private int solvedScore;
    private int contestScore;
    private LocalDateTime computedAt;
}
