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
}
