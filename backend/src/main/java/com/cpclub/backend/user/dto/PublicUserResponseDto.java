package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * Public-safe projection of a {@link User} for unauthenticated endpoints.
 *
 * <p>Unlike {@link UserResponseDto}, this DTO deliberately omits the user's
 * email address, system role, and update timestamp to prevent:
 * <ul>
 *   <li>Bulk email harvesting from the public members directory</li>
 *   <li>Exposure of internal authorization roles</li>
 * </ul>
 *
 * <p>Use this on all endpoints that do not require authentication, such as:
 * <ul>
 *   <li>{@code GET /api/users} — members directory</li>
 *   <li>{@code GET /api/users/{id}} — public profile view</li>
 * </ul>
 *
 * @param id              user identifier
 * @param name            member display name
 * @param codeforcesHandle linked external account, if any
 * @param rating          last synchronized Codeforces rating, if any
 * @param createdAt       profile creation timestamp (shows member since)
 */
public record PublicUserResponseDto(
        Long id,
        String name,
        String codeforcesHandle,
        Integer rating,
        LocalDateTime createdAt
) {
    /**
     * Maps only the public-safe fields from a user entity.
     *
     * @param user persisted user entity
     * @return public-safe immutable user response
     */
    public static PublicUserResponseDto fromEntity(User user) {
        return new PublicUserResponseDto(
                user.getId(),
                user.getName(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getCreatedAt()
        );
    }
}
