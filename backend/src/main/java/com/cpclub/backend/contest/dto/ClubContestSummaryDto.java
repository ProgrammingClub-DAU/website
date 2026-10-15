package com.cpclub.backend.contest.dto;

import com.cpclub.backend.contest.entity.ClubContestKind;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** All timestamps are UTC (D31). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestSummaryDto {
    private Long id;
    private Integer cfContestId;
    private ClubContestKind kind;
    private String name;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private int durationSeconds;
    private Long seasonId;
    private String seasonName;
    private LocalDateTime lastSyncedAt;
    private long participantCount;
    /** Where the contest lives on Codeforces. */
    private String url;
}
