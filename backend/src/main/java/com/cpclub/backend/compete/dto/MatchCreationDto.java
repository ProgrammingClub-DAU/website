package com.cpclub.backend.compete.dto;

import com.cpclub.backend.compete.entity.MatchMode;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class MatchCreationDto {
    private LocalDateTime startTime;
    private Integer durationMinutes;
    private Integer minRating;
    private Integer maxRating;
    private MatchMode mode;
    private Integer gridSize;
    private Integer replaceIncrement;
    private List<TeamDto> teams;
    private Integer timeoutMinutes;
    private Boolean showRatings;

    @Data
    public static class TeamDto {
        private String name;
        private String color;
        private List<String> members;
    }
}
