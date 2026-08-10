package com.cpclub.backend.common.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Endpoints for service health checks")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check backend API status")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> status = Map.of(
                "status", "UP",
                "service", "CP Club Backend API",
                "version", "1.0.0"
        );
        return ResponseEntity.ok(ApiResponse.success(status, "Backend service is healthy"));
    }
}
