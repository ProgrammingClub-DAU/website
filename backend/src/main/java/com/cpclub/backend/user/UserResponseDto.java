package com.cpclub.backend.user;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;


import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String name,
        String codeforcesHandle,
        Integer rating,
        Role role,
        LocalDateTime createdAt
) {
    public static UserResponseDto fromEntity(User user) {
        if (user == null) return null;
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
