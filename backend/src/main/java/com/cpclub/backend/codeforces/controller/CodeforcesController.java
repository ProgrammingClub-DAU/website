package com.cpclub.backend.codeforces.controller;

import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/codeforces")
@RequiredArgsConstructor
@Tag(name = "Codeforces Sync", description = "Endpoints for triggering Codeforces rating synchronization")
public class CodeforcesController {

    private final CodeforcesSyncService codeforcesSyncService;

    @PostMapping("/sync")
    @Operation(summary = "Manually trigger Codeforces ratings sync (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> triggerSync() {
        codeforcesSyncService.syncCodeforcesRatings();
        return ResponseEntity.ok(ApiResponse.success("Codeforces synchronization triggered successfully", "Sync complete"));
    }
}
