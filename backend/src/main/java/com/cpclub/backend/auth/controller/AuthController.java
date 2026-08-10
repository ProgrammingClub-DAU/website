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

/**
 * REST controller handling authentication requests including user registration,
 * credential validation (login), and fetching current session details.
 * Communicates with {@link AuthService} for core security operations and token vending.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and identity verification")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Registers a new user account.
     * Checks for duplicate credentials and generates a secure salted password hash via BCrypt.
     *
     * @param registerRequest details of the new student account
     * @return payload containing generated JWT access token and user metadata
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new student account")
    public ResponseEntity<ApiResponse<AuthResponse>> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    /**
     * Authenticates an existing user's email and password.
     * Standardizes login using Spring Security's AuthenticationManager and returns JWT.
     *
     * @param loginRequest login credentials
     * @return payload containing generated JWT access token and user metadata
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate user and receive JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.authenticateUser(loginRequest);
        return ResponseEntity.ok(ApiResponse.success(response, "User authenticated successfully"));
    }

    /**
     * Resolves the details of the currently authenticated user from the JWT SecurityContext.
     *
     * @param userDetails injected spring security context details of the caller
     * @return public user profile details
     */
    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile details")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto userResponse = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(userResponse, "Current user details retrieved successfully"));
    }
}

