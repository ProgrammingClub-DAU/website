package com.cpclub.backend.auth.dto;

import com.cpclub.backend.entity.Role;

public record AuthResponse(
    String token,
    String type,
    Long id,
    String name,
    String email,
    Role role,
    String codeforcesHandle
) {
    public AuthResponse(String token, Long id, String name, String email, Role role, String codeforcesHandle) {
        this(token, "Bearer", id, name, email, role, codeforcesHandle);
    }
}
