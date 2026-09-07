package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;

import java.time.LocalDateTime;

/**
 * Public view of an event, as shown in a list.
 *
 * <p>Carries the organiser's name but not their id or email: the page says who ran
 * the event, and nothing on it links to their profile.</p>
 *
 * @param id event identifier
 * @param title event name
 * @param description long-form details
 * @param eventDate when it runs
 * @param location where it runs
 * @param status upcoming, completed or cancelled
 * @param coverImageUrl card image
 * @param createdByName display name of the admin who created it
 * @param createdAt creation timestamp
 */
public record EventResponseDto(
        Long id,
        String title,
        String description,
        LocalDateTime eventDate,
        String location,
        EventStatus status,
        String coverImageUrl,
        String createdByName,
        LocalDateTime createdAt
) {
    /**
     * Maps a persisted event into its list representation.
     *
     * <p>Reads {@code createdBy}, which is a lazy association, so call this inside
     * the transaction that loaded the event.</p>
     *
     * @param event persisted event
     * @return public event projection
     */
    public static EventResponseDto fromEntity(Event event) {
        return new EventResponseDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getStatus(),
                event.getCoverImageUrl(),
                event.getCreatedBy() != null ? event.getCreatedBy().getName() : null,
                event.getCreatedAt()
        );
    }
}
