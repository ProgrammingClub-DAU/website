package com.cpclub.backend.auth.controller;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.LoginRequest;
import com.cpclub.backend.auth.dto.RegisterRequest;
import com.cpclub.backend.auth.service.AuthService;
import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and identity verification")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "Register a new student account")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "User authenticated successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto userResponse = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(userResponse, "Current user details retrieved successfully"));
    }
}
