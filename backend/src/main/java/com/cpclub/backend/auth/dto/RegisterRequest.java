package com.cpclub.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable registration payload with boundary validation at the HTTP edge.
 *
 * <p>Validation keeps malformed identity data out of the service layer; the service
 * remains responsible for database-backed uniqueness checks and password hashing.</p>
 *
 * @param name member display name
 * @param email student email address used for sign-in
 * @param password plaintext password to be BCrypt-hashed before persistence
 * @param codeforcesHandle optional unique Codeforces account handle
 */
public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be at least 6 characters long")
        String password,

        @Size(max = 100, message = "Codeforces handle must be at most 100 characters")
        String codeforcesHandle
) {
}
