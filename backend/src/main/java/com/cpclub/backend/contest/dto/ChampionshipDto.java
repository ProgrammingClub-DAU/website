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
public class ChampionshipDto {
    private SeasonDto season;
    private List<Row> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        /** Shared when total, contests with points and best rank are all equal. */
        private int rank;
        private MemberRefDto member;
        private long total;
        private int contestsWithPoints;
        private int bestRank;
        private List<ContestScore> breakdown;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContestScore {
        private Long clubContestId;
        private String contestName;
        private int clubRank;
        private int points;
        /** Whether this contest is one of the member's best K that make up the total. */
        private boolean counted;
    }
}
