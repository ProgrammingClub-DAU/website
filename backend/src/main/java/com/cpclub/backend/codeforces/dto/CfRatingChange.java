package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry of Codeforces {@code user.rating}: a rated contest and its rating change. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfRatingChange {
    private Integer contestId;
    private String contestName;
    private String handle;
    private Integer rank;
    private Long ratingUpdateTimeSeconds;
    private Integer oldRating;
    private Integer newRating;
}
