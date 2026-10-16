package com.cpclub.backend.event.dto;

import com.cpclub.backend.user.entity.ClubRole;

import java.time.LocalDateTime;

/**
 * One RSVP, for the admin attendance panel. Admin-only: it carries the member's
 * email and phone number.
 *
 * @param alreadyAttending whether the member is already on the attendance record,
 *                         so the panel can show who still needs marking
 */
public record EventRegistrationDto(
        Long userId,
        String name,
        String email,
        String phoneNumber,
        ClubRole clubRole,
        LocalDateTime registeredAt,
        boolean alreadyAttending
) {
}
