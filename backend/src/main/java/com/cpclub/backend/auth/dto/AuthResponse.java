package com.cpclub.backend.auth.dto;

import com.cpclub.backend.user.entity.Role;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        Role role,
        String codeforcesHandle
) {
}
