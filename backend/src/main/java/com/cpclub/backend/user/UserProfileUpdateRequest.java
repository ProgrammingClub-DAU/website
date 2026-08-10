package com.cpclub.backend.user;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;


public record UserProfileUpdateRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,24}$", message = "Invalid Codeforces handle format")
    String codeforcesHandle
) {}
