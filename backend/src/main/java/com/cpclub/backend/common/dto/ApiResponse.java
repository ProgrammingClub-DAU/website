package com.cpclub.backend.common.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Generic envelope used by controllers and exception handlers for a consistent API shape.
 *
 * <p>Every response supplies an outcome flag, human-readable message, optional payload,
 * and timestamp so frontend clients do not need endpoint-specific error parsing.</p>
 *
 * @param <T> type of the optional successful response payload
 */
@Getter
@Setter
@NoArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    /**
     * Creates a timestamped API envelope.
     *
     * @param success whether the request completed successfully
     * @param message client-readable result description
     * @param data optional response payload
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * Creates a successful response using an explicit message.
     *
     * @param data successful payload
     * @param message client-readable success message
     * @param <T> payload type
     * @return success envelope
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * Creates a successful response with the default success message.
     *
     * @param data successful payload
     * @param <T> payload type
     * @return success envelope
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data);
    }

    /**
     * Creates an error response without exposing an implementation payload.
     *
     * @param message client-readable failure message
     * @param <T> expected endpoint payload type
     * @return failure envelope
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
