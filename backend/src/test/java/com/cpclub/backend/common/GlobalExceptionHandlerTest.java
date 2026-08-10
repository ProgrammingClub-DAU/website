package com.cpclub.backend.common;

import com.cpclub.backend.common.ApiResponse;
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
    @DisplayName("Should map ResourceNotFoundException to ApiResponse with 404 NOT_FOUND")
    void shouldHandleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found with id: 42");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("User not found with id: 42", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    @DisplayName("Should map BadRequestException to ApiResponse with 400 BAD_REQUEST")
    void shouldHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Invalid request data");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequestException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(400, response.getBody().statusCode());
        assertEquals("Invalid request data", response.getBody().message());
    }

    @Test
    @DisplayName("Should map ResponseStatusException to ApiResponse with matching status")
    void shouldHandleResponseStatusException() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(404, response.getBody().statusCode());
        assertEquals("User not found", response.getBody().message());
    }

    @Test
    @DisplayName("Should map validation errors to 400 Bad Request")
    void shouldHandleValidationException() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "updateHandleRequest");
        bindingResult.addError(new FieldError("updateHandleRequest", "codeforcesHandle", "Invalid Codeforces handle format"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(400, response.getBody().statusCode());
        assertTrue(response.getBody().message().contains("Validation failed"));
        assertTrue(response.getBody().message().contains("codeforcesHandle"));
    }

    @Test
    @DisplayName("Should map unexpected exceptions to 500 without exposing internals")
    void shouldHandleGeneralException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneralException(new RuntimeException("database down"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().success());
        assertEquals(500, response.getBody().statusCode());
        assertEquals("An unexpected internal error occurred", response.getBody().message());
        assertFalse(response.getBody().message().contains("database down"));
    }
}
