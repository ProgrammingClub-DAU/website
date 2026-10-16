package com.cpclub.backend.contest.dto;

import com.cpclub.backend.contest.entity.ClubContestKind;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestCreateRequest {
    @NotNull(message = "Codeforces contest id is required")
    @Positive(message = "Codeforces contest id must be positive")
    private Integer cfContestId;

    @NotNull(message = "Contest kind is required")
    private ClubContestKind kind;

    /** Optional: a contest can sit outside every season. */
    private Long seasonId;
}
