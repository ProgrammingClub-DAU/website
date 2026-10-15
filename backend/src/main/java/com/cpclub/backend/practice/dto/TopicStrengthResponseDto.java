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
}
