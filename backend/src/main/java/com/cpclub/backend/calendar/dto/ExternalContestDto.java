package com.cpclub.backend.calendar.dto;

import com.cpclub.backend.calendar.entity.ContestPlatform;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ExternalContestDto {
    private String id;
    private ContestPlatform platform;
    private String name;
    private String url;
    private LocalDateTime startsAt;
    private int durationSeconds;
}
