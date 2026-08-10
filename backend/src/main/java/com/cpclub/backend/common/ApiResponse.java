package com.cpclub.backend.common;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    boolean success,
    int statusCode,
    String message,
    T data,
    LocalDateTime timestamp
) {
    public ApiResponse {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message, HttpStatus status) {
        ApiResponse<T> response = new ApiResponse<>(
                true,
                status.value(),
                message,
                data,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, status);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return success(data, message, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return success(data, "Success", HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(String message, HttpStatus status) {
        ApiResponse<T> response = new ApiResponse<>(
                false,
                status.value(),
                message,
                null,
                LocalDateTime.now()
        );
        return new ResponseEntity<>(response, status);
    }
}
