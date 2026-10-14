package com.cpclub.backend.score.dto;

import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.score.entity.AwardType;
import com.cpclub.backend.user.entity.ClubRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAwardDto {
    private Long id;
    private LocalDate weekStart;
    private AwardType awardType;
    private Long userId;
    private String userName;
    private String codeforcesHandle;
    private String leetcodeHandle;
    private String avatarUrl;
    private ClubRole clubRole;
    private int metricValue;
    private Platform platform;
    private LocalDateTime computedAt;
}
