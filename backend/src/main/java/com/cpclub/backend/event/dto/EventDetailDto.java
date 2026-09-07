package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Public view of a single event, with its gallery and headcount.
 *
 * <p>Composes {@link EventResponseDto} rather than extending it — a record cannot
 * be extended — and builds itself from one, so the list and detail shapes cannot
 * drift apart.</p>
 *
 * <p>Carries the attendee <em>count</em> and not the attendees. The list includes
 * phone numbers and is admin-only; the count is what a public page shows.</p>
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
 * @param photos the event gallery, in upload order
 * @param attendeeCount how many members attended
 */
public record EventDetailDto(
        Long id,
        String title,
        String description,
        LocalDateTime eventDate,
        String location,
        EventStatus status,
        String coverImageUrl,
        String createdByName,
        LocalDateTime createdAt,
        List<EventPhotoDto> photos,
        Integer attendeeCount
) {
    /**
     * Combines an event with its gallery and headcount.
     *
     * @param event persisted event
     * @param photos the event photos
     * @param attendeeCount number of attendees recorded
     * @return detail projection
     */
    public static EventDetailDto of(Event event, List<EventPhotoDto> photos, Integer attendeeCount) {
        EventResponseDto base = EventResponseDto.fromEntity(event);
        return new EventDetailDto(
                base.id(),
                base.title(),
                base.description(),
                base.eventDate(),
                base.location(),
                base.status(),
                base.coverImageUrl(),
                base.createdByName(),
                base.createdAt(),
                photos,
                attendeeCount
        );
    }
}
