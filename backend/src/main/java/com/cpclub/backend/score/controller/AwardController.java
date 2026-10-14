package com.cpclub.backend.score.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.score.dto.LatestWeeklyAwardsResponseDto;
import com.cpclub.backend.score.dto.WeeklyAwardDto;
import com.cpclub.backend.score.service.WeeklyAwardService;
import com.cpclub.backend.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Awards", description = "Endpoints for weekly awards and hall of fame")
public class AwardController {

    private final WeeklyAwardService weeklyAwardService;

    @GetMapping("/api/awards/weekly/latest")
    @Operation(summary = "Get the latest weekly awards")
    public ResponseEntity<ApiResponse<LatestWeeklyAwardsResponseDto>> getLatestWeeklyAwards() {
        LatestWeeklyAwardsResponseDto response = weeklyAwardService.getLatestWeeklyAwards();
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched latest weekly awards successfully"));
    }

    @GetMapping("/api/awards/weekly/{weekStart}")
    @Operation(summary = "Get weekly awards for a specific week")
    public ResponseEntity<ApiResponse<List<WeeklyAwardDto>>> getAwardsByWeek(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart
    ) {
        List<WeeklyAwardDto> response = weeklyAwardService.getAwardsByWeek(weekStart);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched weekly awards successfully"));
    }

    @GetMapping("/api/awards/users/{id}")
    @Operation(summary = "Get a member's award history")
    public ResponseEntity<ApiResponse<List<WeeklyAwardDto>>> getUserAwards(@PathVariable Long id) {
        List<WeeklyAwardDto> response = weeklyAwardService.getUserAwards(id);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched member awards successfully"));
    }

    @PostMapping("/api/admin/awards/weekly/{weekStart}/recompute")
    @Operation(summary = "Recompute awards for a closed week (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WeeklyAwardDto>>> recomputeWeeklyAwards(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long adminId = userDetails != null ? userDetails.getId() : null;
        List<WeeklyAwardDto> response = weeklyAwardService.computeWeek(weekStart, adminId);
        return ResponseEntity.ok(ApiResponse.success(response, "Recomputed weekly awards successfully"));
    }
}
