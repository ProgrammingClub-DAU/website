package com.cpclub.backend.contest.dto;

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
}
