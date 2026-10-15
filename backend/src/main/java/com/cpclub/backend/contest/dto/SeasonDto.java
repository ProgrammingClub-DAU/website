package com.cpclub.backend.contest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonDto {
    private Long id;
    private String name;
    private LocalDate startsOn;
    private LocalDate endsOn;
    private Integer countBest;
    private long contestCount;
}
