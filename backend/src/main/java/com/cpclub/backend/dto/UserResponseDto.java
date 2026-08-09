package com.cpclub.backend.dto;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private String codeforcesHandle;
    private Integer rating;
    private Role role;
    private LocalDateTime createdAt;

    public static UserResponseDto fromEntity(User user) {
        if (user == null) return null;
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .codeforcesHandle(user.getCodeforcesHandle())
                .rating(user.getRating())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
