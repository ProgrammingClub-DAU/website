package com.cpclub.backend.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record UpdateHandleRequest(
    @NotBlank(message = "Codeforces handle cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,24}$", message = "Invalid Codeforces handle format")
    String codeforcesHandle
) {}
