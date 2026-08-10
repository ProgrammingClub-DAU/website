package com.cpclub.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 100, message = "Codeforces handle must be at most 100 characters")
        String codeforcesHandle
) {
}
