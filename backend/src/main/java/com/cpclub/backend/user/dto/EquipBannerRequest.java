package com.cpclub.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipBannerRequest(
        @NotBlank(message = "Banner ID is required")
        @Size(max = 50, message = "Banner ID must not exceed 50 characters")
        String bannerId
) {}
