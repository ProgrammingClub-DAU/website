package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Head-to-head record across shared rated contests (P6, D25).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeadToHeadDto {
    private int aAhead;
    private int bAhead;
    private int tied;
    private List<HeadToHeadContestDto> recentContests;
}
