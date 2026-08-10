package com.cpclub.backend.common.exception;

import com.cpclub.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
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
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password!"));
    }

    /**
     * Maps authenticated-but-forbidden requests to HTTP 403.
     *
     * @param ex authorization failure
     * @return standardized forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: You do not have permission to perform this action."));
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
     * Provides a final consistent error response for unexpected failures.
     *
     * @param ex unhandled exception
     * @return standardized internal-server-error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred: " + ex.getMessage()));
    }
}
