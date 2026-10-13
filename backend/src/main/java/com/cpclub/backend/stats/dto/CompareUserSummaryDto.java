package com.cpclub.backend.stats.dto;

import com.cpclub.backend.user.entity.ClubRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public summary of a member for the comparison view.
 * Deliberately excludes private details like email or phone (D25).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareUserSummaryDto {
    private Long id;
    private String name;
    private String codeforcesHandle;
    private String leetcodeHandle;
    private String avatarUrl;
    private ClubRole clubRole;
}
