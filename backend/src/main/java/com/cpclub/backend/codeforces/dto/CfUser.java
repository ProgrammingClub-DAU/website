package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfUser {
    private String handle;
    private Integer rating;
    private Integer maxRating;
    private String rank;
    private String maxRank;
}
