package com.cpclub.backend.contest.dto;

import com.cpclub.backend.user.entity.AcademicYear;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public identity of a club member: never email or phone (D25). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRefDto {
    private Long id;
    private String name;
    private String avatarUrl;
    private String codeforcesHandle;
    /** For the per-year prize podiums; null when the member has not set it. Public on profiles too. */
    private AcademicYear academicYear;
}
