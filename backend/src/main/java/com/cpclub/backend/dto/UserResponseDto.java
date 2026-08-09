package com.cpclub.backend.dto;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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
        if (user == null) {
            return null;
        }
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}
