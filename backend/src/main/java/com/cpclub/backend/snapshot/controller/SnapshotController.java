package com.cpclub.backend.snapshot.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.snapshot.dto.RatingPointDto;
import com.cpclub.backend.snapshot.service.SnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller serving the rating history behind the profile charts.
 *
 * <p>Both endpoints require an authenticated caller. The rule is also declared in
 * {@code SecurityConfig}; the {@link PreAuthorize} annotations here are the second
 * layer, so a future reordering of the filter chain cannot quietly expose them.</p>
 */
@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
@Tag(name = "Snapshots", description = "Weekly rating history powering the profile rating charts")
public class SnapshotController {

    private final SnapshotService snapshotService;

    /**
     * Resolves a member's Codeforces rating history in chronological order.
     *
     * @param userId member whose history is requested
     * @return chart points, empty when no snapshot has been recorded yet
     */
    @GetMapping("/{userId}/codeforces")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a member's Codeforces rating history")
    public ResponseEntity<ApiResponse<List<RatingPointDto>>> getCodeforcesHistory(
            @PathVariable Long userId
    ) {
        List<RatingPointDto> history = snapshotService.getCodeforcesHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history, "Fetched Codeforces rating history successfully"));
    }

    /**
     * Resolves a member's LeetCode rating history in chronological order.
     *
     * @param userId member whose history is requested
     * @return chart points, empty when no snapshot has been recorded yet
     */
    @GetMapping("/{userId}/leetcode")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a member's LeetCode rating history")
    public ResponseEntity<ApiResponse<List<RatingPointDto>>> getLeetcodeHistory(
            @PathVariable Long userId
    ) {
        List<RatingPointDto> history = snapshotService.getLeetcodeHistory(userId);
        return ResponseEntity.ok(ApiResponse.success(history, "Fetched LeetCode rating history successfully"));
    }
}
