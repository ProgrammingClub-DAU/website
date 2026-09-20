package com.cpclub.backend.compete.dto;

import lombok.Data;
import java.util.List;

@Data
public class SolveReportDto {
    private String handle;
    private Integer contestId;
    private String index;
    private Long creationTimeSeconds;  // Codeforces submission epoch timestamp
    private List<String> solvedKeys;   // Optional: for Replace mode replacement generation
}
