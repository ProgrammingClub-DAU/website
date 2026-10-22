package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry of Codeforces {@code contest.list}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CfContest {
    private Integer id;
    private String name;
    private String type;
    /** BEFORE, CODING, PENDING_SYSTEM_TEST, SYSTEM_TEST or FINISHED. */
    private String phase;
    private Integer durationSeconds;
    /** Absent for contests that have not been scheduled yet. */
    private Long startTimeSeconds;
}
