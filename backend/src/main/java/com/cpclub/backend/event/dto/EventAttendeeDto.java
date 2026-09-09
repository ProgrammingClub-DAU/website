package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * One row of an event attendance list, for the admin panel and the Excel export.
 *
 * <p>Admin-only. It carries phone numbers and email addresses for the whole
 * attendance list, which is the club contact sheet for that event.</p>
 *
 * @param userId member identifier
 * @param name member display name
 * @param email member email address
 * @param phoneNumber contact number; null for members who joined before Phase 2
 * @param hasPhone whether a usable phone number is on file
 * @param avatarUrl profile photo URL
 * @param codeforcesHandle linked Codeforces account
 * @param cfRating last synced Codeforces rating
 * @param leetcodeHandle linked LeetCode account
 * @param leetcodeRating last synced LeetCode contest rating
 * @param codechefUrl CodeChef profile link
 * @param atcoderUrl AtCoder profile link
 * @param githubUrl GitHub profile link
 * @param linkedinUrl LinkedIn profile link
 * @param clubRole position held in the club
 * @param addedAt when the admin recorded this attendance
 */
public record EventAttendeeDto(
        Long userId,
        String name,
        String email,
        String phoneNumber,
        boolean hasPhone,
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
        LocalDateTime addedAt
) {
    /**
     * Maps an attendance row into its admin representation.
     *
     * <p>Reads the {@code user} association, which is lazy — the repository query
     * that loads attendees joins it in for this reason.</p>
     *
     * @param attendee persisted attendance row
     * @return admin-facing attendee projection
     */
    public static EventAttendeeDto fromEntity(EventAttendee attendee) {
        User user = attendee.getUser();
        String phone = user.getPhoneNumber();
        return new EventAttendeeDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                phone,
                phone != null && !phone.isBlank(),
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
                attendee.getAddedAt()
        );
    }
}
