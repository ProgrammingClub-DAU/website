package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.AcademicYear;
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
 * @param maxRating highest Codeforces rating reached
 * @param equippedBannerId currently selected leaderboard banner
 * @param isPlatformCreator whether the member may use the creator VIP banner
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
        AcademicYear academicYear,
        /**
         * Whether this member has filled in everything the club needs.
         *
         * <p>Computed rather than stored: the answer changes the moment any of
         * those fields changes, and a stored copy would drift.</p>
         */
        boolean profileComplete,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer maxRating,
        String equippedBannerId,
        @com.fasterxml.jackson.annotation.JsonProperty("isPlatformCreator")
        boolean isPlatformCreator
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
                user.getAcademicYear(),
                isProfileComplete(user),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getMaxRating(),
                user.getEquippedBannerId(),
                user.isPlatformCreator()
        );
    }

    /**
     * The four things the club needs from every member.
     *
     * <p>Name and Codeforces handle put them on the leaderboard, the phone number
     * is how an organiser reaches them at an event, and the year decides which
     * sessions are meant for them. Blank strings count as missing: a space is not
     * a phone number.</p>
     *
     * @param user the member to check
     * @return true when nothing is outstanding
     */
    private static boolean isProfileComplete(User user) {
        return notBlank(user.getName())
                && notBlank(user.getCodeforcesHandle())
                && notBlank(user.getPhoneNumber())
                && user.getAcademicYear() != null;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
