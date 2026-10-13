package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Single contest rating data point for multi-platform rating charts (O10, 4.9).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingPointDto {
    private LocalDateTime date;
    private int rating;
    private String contestName;
    private String contestId;
    private Integer rank;
}
