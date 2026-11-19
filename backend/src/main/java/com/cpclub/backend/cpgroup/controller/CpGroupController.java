package com.cpclub.backend.cpgroup.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.cpgroup.dto.CpGroupRequests;
import com.cpclub.backend.cpgroup.service.CpGroupService;
import com.cpclub.backend.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The one thing a student sees of the CP Practice Group: whether they are in it. */
@RestController
@RequestMapping("/api/cp-group")
@RequiredArgsConstructor
@Tag(name = "CP Practice Group", description = "A student's own membership")
public class CpGroupController {

    private final CpGroupService cpGroupService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Whether the signed-in student is in the CP Practice Group")
    public ResponseEntity<ApiResponse<CpGroupRequests.MyMembership>> myMembership(
            @AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(ApiResponse.success(cpGroupService.myMembership(user.getId()), "Fetched membership"));
    }
}
