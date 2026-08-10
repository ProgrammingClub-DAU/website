package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
