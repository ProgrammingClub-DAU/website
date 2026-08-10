package com.cpclub.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank(message = "Email cannot be empty")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password cannot be empty")
    String password
) {}
