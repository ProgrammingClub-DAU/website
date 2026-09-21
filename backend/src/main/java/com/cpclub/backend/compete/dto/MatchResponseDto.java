package com.cpclub.backend.compete.dto;

import com.cpclub.backend.compete.entity.MatchMode;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class MatchResponseDto {
    private String id;
    private Instant startTime;
    private Integer durationMinutes;
    private MatchMode mode;
    private Integer replaceIncrement;
    private Integer gridSize;
    private Integer timeoutMinutes;
    private Boolean showRatings;
    
    private List<TeamDto> teams;
    private List<ProblemCellDto> problems;
    private List<SolveEntryDto> solveLog;

    @Data
    @Builder
    public static class TeamDto {
        private String name;
        private String color;
        private List<String> members;
    }

    @Data
    @Builder
    public static class ProblemCellDto {
        private Integer row;
        private Integer col;
        private Integer contestId;
        private String index;
        private String name;
        private Integer rating;
        private String link;
        private String claimedBy; // Handle or team name
        private String solvedBy; // Team name
        private Boolean active;
        private Integer position;
    }

    @Data
    @Builder
    public static class SolveEntryDto {
        private String handle;
        private String team;
        private Instant timestamp;
        private ProblemRefDto problem;
    }

    @Data
    @Builder
    public static class ProblemRefDto {
        private Integer contestId;
        private String index;
        private String name;
        private Integer position;
    }
}
