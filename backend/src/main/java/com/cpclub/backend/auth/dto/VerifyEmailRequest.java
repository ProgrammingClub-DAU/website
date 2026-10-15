package com.cpclub.backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
        @NotBlank(message = "Token is required")
        @Schema(description = "Single-use verification token received via email", example = "4uRz_aBC...")
        String token
) {}
