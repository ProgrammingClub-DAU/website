package com.cpclub.backend.contest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RanklistRowDto {
    /** Rank among club members only; ties share a rank. */
    private int rank;
    private MemberRefDto member;
    private BigDecimal cfPoints;
    private int penalty;
    private int solvedCount;
    /** One cell per problem, in the same order as the contest's problem list. */
    private List<Cell> cells;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cell {
        private String problemIndex;
        private boolean solved;
        private boolean attempted;
    }
}
