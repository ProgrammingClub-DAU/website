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
public class PracticeSuggestionsResponseDto {
    private Integer ratingBandMin;
    private Integer ratingBandMax;
    private List<String> weakTags;
    private String reason;
    private List<PracticeSuggestionDto> suggestions;
}
