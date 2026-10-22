package com.cpclub.backend.common.exception;

import com.cpclub.backend.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates known domain, validation, and security exceptions into one stable JSON response contract.
 *
 * <p>Keeping error mapping at this boundary prevents controllers from duplicating HTTP concerns and
 * avoids leaking internal exception details for expected client mistakes.</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maps business-rule violations to HTTP 400.
     *
     * @param ex validation failure raised by the service layer
     * @return standardized bad-request response
     */
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Maps a request the resource's current state forbids to HTTP 409.
     *
     * @param ex conflict raised by the service layer, with a message meant for the admin
     * @return standardized conflict response
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflictException(ConflictException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Maps absent domain resources to HTTP 404.
     *
     * @param ex missing-resource failure
     * @return standardized not-found response
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Maps invalid login credentials to an intentionally non-specific HTTP 401 response.
     *
     * @param ex credential failure from Spring Security
     * @return standardized unauthorized response
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        // The message is shown to the member. Sign-in is Google-only and the one
        // thing that goes wrong in practice -- signing in with a personal account
        // instead of the university one -- is only fixable if we say so. These
        // messages are written for that, and never quote the token.
        String message = ex.getMessage() != null && !ex.getMessage().isBlank()
                ? ex.getMessage()
                : "That sign-in could not be verified.";

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(message));
    }

    /**
     * Maps authenticated-but-forbidden requests to HTTP 403.
     *
     * @param ex authorization failure
     * @return standardized forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        // A @PreAuthorize denial lands here before Spring Security's own exception
        // filter sees it, so without this check an anonymous caller would get 403.
        // That matters on endpoints the URL rules leave public but the method guards
        // -- GET /api/club-contests/upsolve/me, GET /api/blogs/{id}/edit -- where the
        // honest answer to someone not signed in is 401: sign in, then try again.
        if (isAnonymous(SecurityContextHolder.getContext().getAuthentication())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Unauthorized access: Full authentication is required to access this resource"));
        }
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: You do not have permission to perform this action."));
    }

    private static boolean isAnonymous(Authentication authentication) {
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }

    /**
     * Collects field-level Jakarta validation failures into a client-friendly map.
     *
     * @param ex binding and validation failure raised before controller invocation
     * @return standardized validation response
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(false, "Validation failed", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Maps ResponseStatusException directly to its intended HTTP status and message.
     *
     * @param ex Spring Web ResponseStatusException
     * @return standardized response with correct status code
     */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        // Forward the exception's own headers -- Retry-After on a 429, Allow on a 405 --
        // which were dropped here before, leaving clients nothing to act on.
        return ResponseEntity
                .status(ex.getStatusCode())
                .headers(ex.getHeaders())
                .body(ApiResponse.error(ex.getReason() != null ? ex.getReason() : ex.getMessage()));
    }

    /**
     * A write the database refused: a value longer than its column, a duplicate of a
     * unique value, a missing required field.
     *
     * <p>Answered as 400 with a plain message. It used to fall through to the 500
     * handler below, which put the failing SQL statement into the response.</p>
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Database rejected {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        "Could not save: a value is too long, missing, or already in use. Check the form and try again."));
    }

    /**
     * Provides a final consistent error response for unexpected failures.
     *
     * <p>Logs the full exception -- it logged nothing before, so a 500 left no trace in
     * the server logs at all -- and returns no internals: the exception message used
     * to be echoed to the client, SQL statements included.</p>
     *
     * @param ex unhandled exception
     * @return standardized internal-server-error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred on the server. Please try again in a moment."));
    }
}
