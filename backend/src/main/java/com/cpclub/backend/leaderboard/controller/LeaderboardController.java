package com.cpclub.backend.leaderboard.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import com.cpclub.backend.leaderboard.dto.LeaderboardPlatform;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for retrieving member rankings based on synchronized Codeforces ratings.
 * Open to public access without authentication.
 */
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Validated
@Tag(name = "Leaderboard", description = "Endpoints for viewing public member rankings based on Codeforces ratings")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;
    private final com.cpclub.backend.stats.service.StatsService statsService;

    /**
     * Resolves paginated ranked listing of club members.
     * Ranks are dynamically offset based on the requested page and size parameters.
     *
     * @param page zero-indexed page number
     * @param size page size limit
     * @return payload containing ordered ranking lists
     */
    @GetMapping
    @Operation(summary = "Get ranked member leaderboard (paginated, filterable by platform and club role)")
    public ResponseEntity<ApiResponse<PagedResponse<LeaderboardResponseDto>>> getLeaderboard(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(2000) int size,
            @RequestParam(defaultValue = "CODEFORCES") LeaderboardPlatform platform,
            @RequestParam(defaultValue = "ALL") LeaderboardFilter filter
    ) {
        PagedResponse<LeaderboardResponseDto> response =
                leaderboardService.getLeaderboard(page, size, platform, filter);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched leaderboard successfully"));
    }

    /**
     * Resolves dynamic stats-based leaderboard across metrics, platforms, periods, and club role filters.
     */
    @GetMapping("/stats")
    @Operation(summary = "Get ranked leaderboard across metrics, platforms, periods, and filters")
    public ResponseEntity<ApiResponse<com.cpclub.backend.stats.dto.StatsLeaderboardResponseDto>> getStatsLeaderboard(
            @RequestParam(defaultValue = "SOLVED") com.cpclub.backend.stats.dto.StatsMetric metric,
            @RequestParam(defaultValue = "CODEFORCES") com.cpclub.backend.stats.dto.StatsPlatform platform,
            @RequestParam(defaultValue = "WEEK") com.cpclub.backend.stats.dto.StatsPeriod period,
            @RequestParam(defaultValue = "ALL") com.cpclub.backend.leaderboard.dto.LeaderboardFilter filter,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(2000) int size
    ) {
        com.cpclub.backend.stats.dto.StatsLeaderboardResponseDto response =
                statsService.getLeaderboard(metric, platform, period, filter, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched stats leaderboard successfully"));
    }
}

