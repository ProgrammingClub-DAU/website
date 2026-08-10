package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * Immutable, client-safe projection of a {@link User}.
 *
 * <p>Controllers return this DTO instead of the JPA entity so password hashes and
 * persistence implementation details cannot be serialized accidentally.</p>
 *
 * @param id user identifier
 * @param name member display name
 * @param email member email address
 * @param codeforcesHandle linked external account, if any
 * @param rating last synchronized Codeforces rating, if any
 * @param role application authorization role
 * @param createdAt profile creation timestamp
 * @param updatedAt most recent persistence update timestamp
 */
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
    /**
     * Maps the stable, public fields of a user entity into its API representation.
     *
     * @param user persisted user entity
     * @return client-safe immutable user response
     */
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
