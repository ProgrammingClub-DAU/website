package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

public record UserResponseDto(
        Long id,
        String name,
        String email,
        String codeforcesHandle,
        Integer rating,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserResponseDto fromEntity(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
