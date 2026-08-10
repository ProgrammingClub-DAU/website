package com.cpclub.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable, validated credential payload accepted by the login endpoint.
 *
 * @param email member email used as the authentication principal
 * @param password plaintext password supplied only for authentication
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
