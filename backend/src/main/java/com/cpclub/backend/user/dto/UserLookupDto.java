package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.User;

/**
 * Full member view for the admin event-attendance panel.
 *
 * <p>Wider than {@link UserResponseDto} and much wider than
 * {@link PublicUserResponseDto}: it carries the phone number, which is contact
 * information rather than profile decoration. Every endpoint returning this must
 * be admin-gated — an unauthenticated route handing this back would publish the
 * membership's phone numbers.</p>
 *
 * <p>It deliberately omits {@code role}. An admin picking someone to add to an
 * event has no business knowing their API authorization level, and leaving it out
 * keeps this DTO from becoming a general-purpose admin user dump.</p>
 *
 * @param id user identifier
 * @param name member display name
 * @param email member email address
 * @param phoneNumber contact number; null for members who joined before Phase 2
 * @param avatarUrl profile photo URL
 * @param codeforcesHandle linked Codeforces account
 * @param cfRating last synced Codeforces rating
 * @param leetcodeHandle linked LeetCode account
 * @param leetcodeRating last synced LeetCode contest rating; zero means never contested
 * @param codechefUrl CodeChef profile link
 * @param atcoderUrl AtCoder profile link
 * @param githubUrl GitHub profile link
 * @param linkedinUrl LinkedIn profile link
 * @param clubRole position held in the club
 * @param batchYear admission batch
 */
public record UserLookupDto(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String avatarUrl,
        String codeforcesHandle,
        Integer cfRating,
        String leetcodeHandle,
        Integer leetcodeRating,
        String codechefUrl,
        String atcoderUrl,
        String githubUrl,
        String linkedinUrl,
        ClubRole clubRole,
        Integer batchYear
) {
    /**
     * Maps a persisted member into the admin lookup view.
     *
     * @param user persisted user entity
     * @return full admin-facing projection
     */
    public static UserLookupDto fromEntity(User user) {
        return new UserLookupDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAvatarUrl(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getLeetcodeHandle(),
                user.getLeetcodeRating(),
                user.getCodechefUrl(),
                user.getAtcoderUrl(),
                user.getGithubUrl(),
                user.getLinkedinUrl(),
                user.getClubRole(),
                user.getBatchYear()
        );
    }
}
