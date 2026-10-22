package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/** One entry of {@code problemset.problems}' problemStatistics array. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfProblemStatistics {
    private Integer contestId;
    private String index;
    private Integer solvedCount;
}
