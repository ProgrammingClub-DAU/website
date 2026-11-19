package com.cpclub.backend.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** A problem the member tried on Codeforces and has not solved yet. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TryAgainDto {
    private Long problemId;
    private String problemKey;
    private Integer contestId;
    private String problemIndex;
    private String name;
    private Integer rating;
    private List<String> tags;
    private String url;
    /** The verdict of the latest failed submission, e.g. WRONG_ANSWER. */
    private String lastVerdict;
    /** When it was submitted, UTC (D31). */
    private LocalDateTime lastAttemptAt;
    /** The estimated chance of solving it now, in percent; null when the problem is unrated. */
    private Integer chance;
}
