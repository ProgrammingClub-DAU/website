package com.cpclub.backend.event.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Admin payload for adding one member to an event attendance list.
 *
 * @param userId the member to add
 */
public record AddAttendeeRequest(
        @NotNull(message = "User ID must not be null")
        Long userId
) {
}
