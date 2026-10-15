package com.cpclub.backend.score.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Payload for GET /api/awards/weekly/latest.
 * computed = false means the job has not run yet.
 * computed = true with an empty list means there were no winners.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestWeeklyAwardsResponseDto {
    private LocalDate weekStart;
    private boolean computed;
    private List<WeeklyAwardDto> awards;
}
