package com.cpclub.backend.leaderboard.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for retrieving member rankings based on synchronized Codeforces ratings.
 * Open to public access without authentication.
 */
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Endpoints for viewing public member rankings based on Codeforces ratings")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Resolves paginated ranked listing of club members.
     * Ranks are dynamically offset based on the requested page and size parameters.
     *
     * @param page zero-indexed page number
     * @param size page size limit
     * @return payload containing ordered ranking lists
     */
    @GetMapping
    @Operation(summary = "Get ranked member leaderboard (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<LeaderboardResponseDto>>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<LeaderboardResponseDto> response = leaderboardService.getLeaderboard(page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched leaderboard successfully"));
    }
}

