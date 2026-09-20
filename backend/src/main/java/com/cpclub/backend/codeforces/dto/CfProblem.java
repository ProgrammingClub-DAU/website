package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfProblem {
    private Integer contestId;
    private String index;
    private String name;
    private Integer rating;
    private List<String> tags;
    private String problemsetName;
}
