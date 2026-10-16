package com.cpclub.backend.contest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestDetailDto {
    private ClubContestSummaryDto contest;
    private List<ContestProblemDto> problems;
    private List<RanklistRowDto> rows;
}
