package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * Immutable, client-safe projection of a {@link User}.
 *
 * <p>Controllers return this DTO instead of the JPA entity so password hashes and
 * persistence implementation details cannot be serialized accidentally.</p>
 *
 * <p>This is the authenticated self-view: it carries {@code email},
 * {@code phoneNumber} and {@code role}, so it must not be returned from a public
 * endpoint. The public equivalent is {@link PublicUserResponseDto}.</p>
 *
 * <p>The phone number is here because a member has to see their own in order to
 * edit it. That makes every endpoint returning this DTO a contact-data endpoint:
 * {@code GET /api/users/all} was narrowed from authenticated to admin when this
 * field was added, for exactly that reason.</p>
 *
 * @param id user identifier
 * @param name member display name
 * @param email member email address
 * @param avatarUrl profile photo URL
 * @param phoneNumber contact number; visible to the member themselves and to admins
 * @param codeforcesHandle linked external account, if any
 * @param rating last synchronized Codeforces rating, if any
 * @param leetcodeHandle linked LeetCode account, if any
 * @param leetcodeRating last synced LeetCode contest rating; zero means never contested
 * @param codechefUrl CodeChef profile link
 * @param atcoderUrl AtCoder profile link
 * @param githubUrl GitHub profile link
 * @param linkedinUrl LinkedIn profile link
 * @param clubRole position held in the club, distinct from {@code role}
 * @param batchYear admission batch
 * @param role application authorization role
 * @param createdAt profile creation timestamp
 * @param updatedAt most recent persistence update timestamp
 */
public record UserResponseDto(
        Long id,
        String name,
        String email,
        String avatarUrl,
        String phoneNumber,
        String codeforcesHandle,
        Integer rating,
        String leetcodeHandle,
        Integer leetcodeRating,
        String codechefUrl,
        String atcoderUrl,
        String githubUrl,
        String linkedinUrl,
        ClubRole clubRole,
        Integer batchYear,
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
                user.getAvatarUrl(),
                user.getPhoneNumber(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getLeetcodeHandle(),
                user.getLeetcodeRating(),
                user.getCodechefUrl(),
                user.getAtcoderUrl(),
                user.getGithubUrl(),
                user.getLinkedinUrl(),
                user.getClubRole(),
                user.getBatchYear(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
