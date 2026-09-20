package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.AcademicYear;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * A member as the public sees them.
 *
 * <p>The site is readable without an account -- the members page, a member's
 * profile and the events listing are all open -- so this is the shape most
 * visitors ever get. It carries what a directory entry and a profile page need:
 * the name, the avatar, the club post, and the platform links a visitor might
 * want to follow.</p>
 *
 * <p>It never carries the email address. That is the difference between this and
 * {@link UserResponseDto}, and the reason the directory does not simply reuse
 * that one: a public list of every member's university address is a spam list.</p>
 *
 * <p>The phone number is the one field that depends on who is asking. See
 * {@link #visiblePhone}.</p>
 *
 * @param phoneNumber the member's number, or null when the viewer may not see it
 */
public record PublicUserResponseDto(
        Long id,
        String name,
        String avatarUrl,
        ClubRole clubRole,
        AcademicYear academicYear,
        String codeforcesHandle,
        Integer rating,
        String leetcodeHandle,
        Integer leetcodeRating,
        String codechefUrl,
        String atcoderUrl,
        String githubUrl,
        String linkedinUrl,
        String phoneNumber,
        LocalDateTime createdAt,
        String equippedBannerId,
        boolean isPlatformCreator
) {

    /**
     * Builds the public view of a member.
     *
     * <p>The caller must say whether the viewer is an admin. There is no
     * single-argument version on purpose: the phone number turns on this
     * question, and a default would decide it silently at whichever call site
     * forgot to.</p>
     *
     * @param user the member
     * @param viewerIsAdmin whether the person asking holds ROLE_ADMIN
     * @return the member, with the phone number included only if permitted
     */
    public static PublicUserResponseDto fromEntity(User user, boolean viewerIsAdmin) {
        return new PublicUserResponseDto(
                user.getId(),
                user.getName(),
                user.getAvatarUrl(),
                user.getClubRole(),
                user.getAcademicYear(),
                user.getCodeforcesHandle(),
                user.getRating(),
                user.getLeetcodeHandle(),
                user.getLeetcodeRating(),
                user.getCodechefUrl(),
                user.getAtcoderUrl(),
                user.getGithubUrl(),
                user.getLinkedinUrl(),
                visiblePhone(user, viewerIsAdmin),
                user.getCreatedAt(),
                user.getEquippedBannerId() != null ? user.getEquippedBannerId() : "rookie",
                user.isPlatformCreator()
        );
    }

    /**
     * Decides whether this member's phone number may be shown.
     *
     * <p>Two rules, and they come from different places. A member holding a club
     * post -- Convenor down to Batch Representative -- is a point of contact for
     * that post, and the club lists them as one; their number is on their profile
     * for anyone who needs to reach the club. Everyone else's number was given so
     * that an organiser could call them about an event, which is a reason for
     * admins to have it and nobody else.</p>
     *
     * <p>Filtered here rather than in the frontend. A number sent to the browser
     * and then hidden by a component is still a number in the page source, in the
     * network tab, and in anything that scrapes the API.</p>
     *
     * @param user the member whose number is in question
     * @param viewerIsAdmin whether the person asking holds ROLE_ADMIN
     * @return the number, or null if this viewer may not have it
     */
    private static String visiblePhone(User user, boolean viewerIsAdmin) {
        if (viewerIsAdmin) {
            return user.getPhoneNumber();
        }

        ClubRole role = user.getClubRole();
        return role != null && role.isOfficeBearer() ? user.getPhoneNumber() : null;
    }
}
