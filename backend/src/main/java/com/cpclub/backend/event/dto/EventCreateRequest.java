package com.cpclub.backend.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Admin payload for creating or updating an event.
 *
 * <p>Used for both, so an update carries the whole event rather than a patch of
 * changed fields. That keeps "the event is exactly this" as the request's meaning,
 * instead of letting an omitted field silently clear a value.</p>
 *
 * <p>The date is deliberately not constrained to the future. Events are sometimes
 * recorded after they happen, and rejecting a past date would make the club's own
 * history unenterable.</p>
 *
 * @param title event name
 * @param description optional long-form details
 * @param eventDate when it runs
 * @param location where it runs
 * @param coverImageUrl optional Cloudinary URL for the card image
 */
public record EventCreateRequest(
        @NotBlank(message = "Title must not be blank")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        @NotNull(message = "Event date must not be null")
        LocalDateTime eventDate,

        @NotBlank(message = "Location must not be blank")
        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        @Size(max = 512, message = "Cover image URL must be at most 512 characters")
        String coverImageUrl
) {
}
