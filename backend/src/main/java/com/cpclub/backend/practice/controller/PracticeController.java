package com.cpclub.backend.practice.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.practice.dto.PracticeSuggestionsResponseDto;
import com.cpclub.backend.practice.service.PracticeSuggestionService;
import com.cpclub.backend.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/practice")
@RequiredArgsConstructor
@Tag(name = "Practice", description = "Endpoints for practice suggestions and problem skipping")
public class PracticeController {

    private final PracticeSuggestionService practiceSuggestionService;

    @GetMapping("/suggestions")
    @Operation(summary = "Get personalized practice suggestions for the current member")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PracticeSuggestionsResponseDto>> getSuggestions(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        PracticeSuggestionsResponseDto suggestions = practiceSuggestionService.getSuggestions(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success(suggestions, "Fetched practice suggestions successfully"));
    }

    @PostMapping("/suggestions/{problemId}/skip")
    @Operation(summary = "Permanently skip a practice suggestion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> skipSuggestion(
            @PathVariable Long problemId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        practiceSuggestionService.skip(userDetails.getId(), problemId);
        return ResponseEntity.noContent().build();
    }
}
