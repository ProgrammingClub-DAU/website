package com.cpclub.backend.contest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonRequest {
    @NotBlank(message = "Season name is required")
    @Size(max = 100, message = "Season name must be at most 100 characters")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startsOn;

    @NotNull(message = "End date is required")
    private LocalDate endsOn;

    /** Best K contests counted toward the total; null counts every contest (Section 1.3). */
    @Positive(message = "Contests counted must be positive")
    private Integer countBest;
}
