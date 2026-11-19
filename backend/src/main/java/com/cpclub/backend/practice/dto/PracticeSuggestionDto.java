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
public class PracticeSuggestionDto {
    private Long problemId;
    private String problemKey;
    private Integer contestId;
    private String problemIndex;
    private String name;
    private Integer rating;
    private Integer solvedCount;
    private List<String> tags;
    private String url;
    /** The focus topic this problem was picked to build; null when picked for the overall level. */
    private String focusTag;
    /** The member's level in that topic, or their overall level. */
    private Integer focusLevel;
    /** The estimated chance of solving it, in percent. */
    private Integer chance;
}
