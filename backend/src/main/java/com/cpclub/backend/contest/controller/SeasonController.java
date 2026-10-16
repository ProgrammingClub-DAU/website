package com.cpclub.backend.contest.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.contest.dto.ChampionshipDto;
import com.cpclub.backend.contest.dto.SeasonDto;
import com.cpclub.backend.contest.dto.SeasonRequest;
import com.cpclub.backend.contest.service.ChampionshipService;
import com.cpclub.backend.contest.service.SeasonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Championship seasons (O14). Reads are public; writes are admin-only. */
@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
@Tag(name = "Seasons", description = "Club Championship seasons and standings")
public class SeasonController {

    private final SeasonService seasonService;
    private final ChampionshipService championshipService;

    @GetMapping
    @Operation(summary = "All seasons, newest first")
    public ResponseEntity<ApiResponse<List<SeasonDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success(seasonService.list(), "Fetched seasons successfully"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a season (admin)")
    public ResponseEntity<ApiResponse<SeasonDto>> create(@Valid @RequestBody SeasonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(seasonService.create(request), "Season created"));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a season (admin)")
    public ResponseEntity<ApiResponse<SeasonDto>> update(@PathVariable Long id, @Valid @RequestBody SeasonRequest request) {
        return ResponseEntity.ok(ApiResponse.success(seasonService.update(id, request), "Season updated"));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a season; its contests are kept (admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        seasonService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Season deleted"));
    }

    @GetMapping("/{id:[0-9]+}/championship")
    @Operation(summary = "Championship standings for a season")
    public ResponseEntity<ApiResponse<ChampionshipDto>> championship(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(championshipService.getStandings(id), "Fetched championship standings successfully"));
    }
}
