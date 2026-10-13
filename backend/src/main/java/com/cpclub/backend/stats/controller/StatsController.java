package com.cpclub.backend.stats.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.stats.dto.CompareDto;
import com.cpclub.backend.stats.dto.MemberStatsDto;
import com.cpclub.backend.stats.dto.RatingPointDto;
import com.cpclub.backend.stats.dto.StatsPeriod;
import com.cpclub.backend.stats.dto.StatsPlatform;
import com.cpclub.backend.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller exposing member and comparison statistics endpoints.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@Validated
@Tag(name = "Stats", description = "Endpoints for retrieving member statistics, rating histories, and comparisons")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/users/{id}")
    @Operation(summary = "Get aggregated statistics for a member over a given period")
    public ResponseEntity<ApiResponse<MemberStatsDto>> getMemberStats(
            @PathVariable Long id,
            @RequestParam(defaultValue = "ALL_TIME") StatsPeriod period
    ) {
        MemberStatsDto stats = statsService.getMemberStats(id, period);
        return ResponseEntity.ok(ApiResponse.success(stats, "Fetched member stats successfully"));
    }

    @GetMapping("/users/{id}/rating-history")
    @Operation(summary = "Get chronological contest rating history for a member on a platform")
    public ResponseEntity<ApiResponse<List<RatingPointDto>>> getRatingHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "CODEFORCES") StatsPlatform platform
    ) {
        List<RatingPointDto> history = statsService.getRatingHistory(id, platform);
        return ResponseEntity.ok(ApiResponse.success(history, "Fetched rating history successfully"));
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare two members head-to-head")
    public ResponseEntity<ApiResponse<CompareDto>> compare(
            @RequestParam Long a,
            @RequestParam Long b
    ) {
        CompareDto comparison = statsService.compare(a, b);
        return ResponseEntity.ok(ApiResponse.success(comparison, "Compared members successfully"));
    }
}
