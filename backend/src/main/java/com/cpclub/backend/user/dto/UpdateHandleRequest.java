package com.cpclub.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated request for linking a Codeforces account to a member profile.
 *
 * @param handle non-blank Codeforces handle, limited to the provider's supported length
 */
public record UpdateHandleRequest(
        @NotBlank(message = "Codeforces handle is required")
        @Size(max = 100, message = "Codeforces handle must be at most 100 characters")
        String handle
) {
}
