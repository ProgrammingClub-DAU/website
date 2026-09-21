package com.cpclub.backend.calendar.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CalendarItemDto {
    private String id;
    private String type; // "CLUB_EVENT", "EXTERNAL_CONTEST"
    private String platform; // "CODEFORCES", etc.
    private String title;
    private String url;
    private LocalDateTime startsAt;
    private LocalDateTime endsAt;
    private String eventType; // Only for club events
}
