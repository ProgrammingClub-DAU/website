package com.cpclub.backend.exception;

import com.cpclub.backend.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should map ResponseStatusException to ErrorResponse with matching status")
    void shouldHandleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with id: 42");

        ResponseEntity<ErrorResponse> response = handler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("User not found with id: 42", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should map validation errors to 400 Bad Request")
    void shouldHandleValidationException() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "updateHandleRequest");
        bindingResult.addError(new FieldError("updateHandleRequest", "codeforcesHandle", "Invalid Codeforces handle format"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertTrue(response.getBody().getMessage().contains("Validation failed"));
        assertTrue(response.getBody().getMessage().contains("codeforcesHandle"));
    }

    @Test
    @DisplayName("Should map unexpected exceptions to 500 without exposing internals")
    void shouldHandleGeneralException() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneralException(new RuntimeException("database down"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("An unexpected internal error occurred", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("database down"));
    }
}
