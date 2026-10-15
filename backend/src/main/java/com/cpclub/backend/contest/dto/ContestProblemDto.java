package com.cpclub.backend.contest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestProblemDto {
    private Long id;
    private String index;
    private String name;
    private Integer rating;
}
