package com.cpclub.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateHandleRequest(
        @NotBlank(message = "Codeforces handle is required")
        @Size(max = 100, message = "Codeforces handle must be at most 100 characters")
        String handle
) {
}
