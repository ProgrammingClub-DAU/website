package com.cpclub.backend.common;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.GlobalExceptionHandler;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
        assertFalse(response.getBody().isSuccess());
        assertEquals("User not found with id: 42", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Should map BadRequestException to ApiResponse with 400 BAD_REQUEST")
    void shouldHandleBadRequestException() {
        BadRequestException ex = new BadRequestException("Invalid request data");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBadRequestException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Invalid request data", response.getBody().getMessage());
    }

    @Test
    @DisplayName("Should map unexpected exceptions to 500")
    void shouldHandleGeneralException() {
        ResponseEntity<ApiResponse<Void>> response = handler.handleGlobalException(new RuntimeException("database down"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("unexpected error occurred"));
    }
}
