package com.cpclub.backend.dto;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;

import java.time.LocalDateTime;

public class UserResponseDto {

    private Long id;
    private String name;
    private String email;
    private String codeforcesHandle;
    private Integer rating;
    private Role role;
    private LocalDateTime createdAt;

    public UserResponseDto() {
    }

    public UserResponseDto(Long id, String name, String email, String codeforcesHandle, Integer rating, Role role, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.codeforcesHandle = codeforcesHandle;
        this.rating = rating;
        this.role = role;
        this.createdAt = createdAt;
    }

    public static UserResponseDto fromEntity(User user) {
        if (user == null) return null;
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeforcesHandle() {
        return codeforcesHandle;
    }

    public void setCodeforcesHandle(String codeforcesHandle) {
        this.codeforcesHandle = codeforcesHandle;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
