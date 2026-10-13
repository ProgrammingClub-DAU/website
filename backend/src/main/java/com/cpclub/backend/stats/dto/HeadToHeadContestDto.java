package com.cpclub.backend.stats.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Details of one shared contest entered by both compared members.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeadToHeadContestDto {
    private String externalContestId;
    private String contestName;
    private LocalDateTime startedAt;
    private Integer rankA;
    private Integer rankB;
}
