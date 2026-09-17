package com.cpclub.backend.halloffame.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.halloffame.dto.HallOfFameEntryDto;
import com.cpclub.backend.halloffame.dto.HallOfFameEntryRequest;
import com.cpclub.backend.halloffame.service.HallOfFameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The Hall of Fame. Reading is public; writing is for admins.
 */
@RestController
@RequestMapping("/api/hall-of-fame")
@RequiredArgsConstructor
@Tag(name = "Hall of Fame", description = "Club achievements, maintained by admins")
public class HallOfFameController {

    private final HallOfFameService hallOfFameService;

    @GetMapping
    @Operation(summary = "List Hall of Fame entries, newest first")
    public ResponseEntity<ApiResponse<List<HallOfFameEntryDto>>> listEntries() {
        return ResponseEntity.ok(ApiResponse.success(
                hallOfFameService.listEntries(), "Fetched Hall of Fame successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one Hall of Fame entry")
    public ResponseEntity<ApiResponse<HallOfFameEntryDto>> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                hallOfFameService.getEntry(id), "Fetched Hall of Fame entry successfully"));
    }

    @PostMapping
    @Operation(summary = "Create a Hall of Fame entry (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HallOfFameEntryDto>> createEntry(
            @Valid @RequestBody HallOfFameEntryRequest request,
            @AuthenticationPrincipal UserDetails admin) {
        HallOfFameEntryDto created = hallOfFameService.createEntry(request, admin.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Hall of Fame entry created successfully"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a Hall of Fame entry, links and photos included (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<HallOfFameEntryDto>> updateEntry(
            @PathVariable Long id,
            @Valid @RequestBody HallOfFameEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                hallOfFameService.updateEntry(id, request), "Hall of Fame entry updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Hall of Fame entry (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long id) {
        hallOfFameService.deleteEntry(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Hall of Fame entry deleted successfully"));
    }
}
