package com.cpclub.backend.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicStrengthResponseDto {
    private Long userId;
    private String codeforcesHandle;
    private String reason;
    private int totalCountedSolves;
    @Builder.Default
    private String note = "A problem with multiple tags counts toward each tag";
    private List<TopicStrengthDto> topics;
    /** The member's estimated overall level; null without a Codeforces handle. */
    private Integer overallLevel;
    /** Distinct rated problems solved: focus topics are picked from 30. */
    private Integer ratedSolves;
    /** The focus topics, weakest first. */
    private List<String> weakTags;
}
