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
public class MyUpsolvesDto {
    /**
     * When the caller's Codeforces submissions were last synced. Upsolve status
     * follows that 6-hourly sync, so an upsolve made since this moment is not shown yet.
     */
    private LocalDateTime cfSyncedAt;
    private List<Contest> contests;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contest {
        private Long clubContestId;
        private String name;
        private LocalDateTime endsAt;
        private List<PendingProblem> pending;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingProblem {
        private String index;
        private String name;
        private Integer rating;
        private String url;
    }
}
