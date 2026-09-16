package com.cpclub.backend.auth.controller;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.GoogleSignInRequest;
import com.cpclub.backend.auth.service.AuthService;
import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Sign-in and identity.
 *
 * <p>There is one way in: a Google account on the club's university domain. The
 * register and password-login endpoints this controller used to expose are gone,
 * along with the accounts-with-passwords model behind them. Signing in for the
 * first time is what creates an account, so there is nothing to register.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Google sign-in and identity verification")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Exchanges a Google ID token for a session on this site.
     *
     * <p>Creates the account if this is the member's first sign-in, so the same
     * call serves both returning and new members. The response carries
     * {@code academicYear}, which is null for a new member and tells the client to
     * ask the welcome question before continuing.</p>
     *
     * @param request the credential issued by Google's sign-in button
     * @return payload containing this site's JWT and user metadata
     */
    @PostMapping("/google")
    @Operation(summary = "Sign in with a university Google account")
    public ResponseEntity<ApiResponse<AuthResponse>> signInWithGoogle(
            @Valid @RequestBody GoogleSignInRequest request) {
        AuthResponse response = authService.signInWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Signed in successfully"));
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
