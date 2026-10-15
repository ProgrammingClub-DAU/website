package com.cpclub.backend.auth.controller;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.GoogleSignInRequest;
import com.cpclub.backend.auth.dto.VerifyEmailRequest;
import com.cpclub.backend.auth.service.AuthService;
import com.cpclub.backend.auth.service.EmailVerificationService;
import com.cpclub.backend.auth.service.ResendRateLimiter;
import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
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
import org.springframework.web.server.ResponseStatusException;

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
    private final EmailVerificationService emailVerificationService;
    private final ResendRateLimiter resendRateLimiter;
    private final UserRepository userRepository;

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

    /**
     * Verifies an email address using a single-use token from the emailed link.
     *
     * @param request body containing token (or query parameter fallback)
     * @param queryToken token provided via query parameter
     * @return 200 OK on success, or 400 Bad Request on failure
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address using a single-use token")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody(required = false) VerifyEmailRequest request,
            @RequestParam(name = "token", required = false) String queryToken) {
        String token = (request != null && request.token() != null && !request.token().isBlank())
                ? request.token()
                : queryToken;
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, EmailVerificationService.INVALID_OR_EXPIRED_MESSAGE);
        }
        emailVerificationService.verify(token);
        return ResponseEntity.ok(ApiResponse.success(null, "Email verified successfully"));
    }

    /**
     * Resends the verification email to the currently authenticated member.
     *
     * <p>Rate-limited in memory (D30) to 1 request per 60 seconds and 5 requests per day.
     * Returns HTTP 429 Too Many Requests with a {@code Retry-After} header when exceeded.</p>
     *
     * @param userDetails authenticated caller principal
     * @return 200 OK on dispatch, or 429 when throttled
     */
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification link")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> resendVerification(java.security.Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        User user = userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        long retryAfter = resendRateLimiter.tryAcquireOrGetRetryAfter(user.getId());
        if (retryAfter > 0) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", String.valueOf(retryAfter))
                    .body(ApiResponse.error("Rate limit exceeded. Try again in " + retryAfter + " seconds"));
        }

        emailVerificationService.issue(user);
        return ResponseEntity.ok(ApiResponse.success(null, "Verification email sent successfully"));
    }
}
