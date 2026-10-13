package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Head-to-head comparison result for two club members (P6, D25).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareDto {
    private CompareUserSummaryDto userA;
    private CompareUserSummaryDto userB;
    private MemberStatsDto statsAAllTime;
    private MemberStatsDto statsBAllTime;
    private MemberStatsDto statsAYear;
    private MemberStatsDto statsBYear;
    private List<RatingPointDto> ratingHistoryACodeforces;
    private List<RatingPointDto> ratingHistoryBCodeforces;
    private List<RatingPointDto> ratingHistoryALeetcode;
    private List<RatingPointDto> ratingHistoryBLeetcode;
    private HeadToHeadDto headToHead;
}
