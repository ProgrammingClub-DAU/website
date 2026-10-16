package com.cpclub.backend.contest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsolveMatrixDto {
    private Long clubContestId;
    private String contestName;
    /** UTC (D31). */
    private LocalDateTime endsAt;
    private List<Problem> problems;
    private List<Row> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Problem {
        private String index;
        private String name;
        private Integer rating;
        /**
         * Of the participants who did not solve it during the contest, the fraction who
         * have upsolved it since. Null when everyone solved it in the contest, since
         * there was nothing left to upsolve.
         */
        private Double upsolveRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Row {
        private MemberRefDto member;
        /** One status per problem, in the same order as the problem list. */
        private List<UpsolveStatus> cells;
    }
}
