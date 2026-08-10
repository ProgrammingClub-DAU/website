package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Administrative request for changing a member's application role.
 *
 * @param role target role; validation prevents null authorization state
 */
public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
