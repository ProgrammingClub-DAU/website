package com.cpclub.backend.compete.dto;

import com.cpclub.backend.compete.entity.MatchMode;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class MatchCreationDto {
    private Instant startTime;
    private Integer durationMinutes;
    private Integer minRating;
    private Integer maxRating;
    private MatchMode mode;
    private Integer gridSize;
    private Integer replaceIncrement;
    private List<TeamDto> teams;
    private Integer timeoutMinutes;
    private Boolean showRatings;
    private List<String> solvedKeys;  // Pre-computed by frontend: ["1234-A", "567-B"]

    @Data
    public static class TeamDto {
        private String name;
        private String color;
        private List<String> members;
    }
}
