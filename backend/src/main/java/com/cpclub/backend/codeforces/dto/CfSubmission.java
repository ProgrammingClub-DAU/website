package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfSubmission {
    private Long id;
    private Integer contestId;
    private Long creationTimeSeconds;
    private String relativeTimeSeconds;
    private CfProblem problem;
    private String programmingLanguage;
    private String verdict;
    private String testset;
    private Integer passedTestCount;
    private Integer timeConsumedMillis;
    private Integer memoryConsumedBytes;
    private CfAuthor author;
}
