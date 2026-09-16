package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.entity.EventType;

import java.time.LocalDateTime;

/**
 * An event as a listing row.
 *
 * <p>The contest link is withheld from the public until an admin publishes it,
 * so a round scheduled for Friday does not leak on Wednesday. The three
 * {@code show*} flags describe the event's publication state rather than its
 * contents, and are sent to everyone: they are what the admin panel renders its
 * switches from, and knowing that an event has results pending is not a secret.</p>
 *
 * @param codeforcesContestUrl the contest, or null when there is none or it is
 *                             not published yet
 */
public record EventResponseDto(
        Long id,
        String title,
        String description,
        LocalDateTime eventDate,
        String location,
        EventStatus status,
        String coverImageUrl,
        EventType eventType,
        String codeforcesContestUrl,
        boolean showContestLink,
        boolean showWinners,
        boolean showAttendeeCount,
        String createdByName,
        LocalDateTime createdAt
) {

    /**
     * Builds the listing row.
     *
     * @param event the event
     * @param viewerIsAdmin whether the caller holds ROLE_ADMIN, who sees the
     *                      contest link regardless of whether it is published
     */
    public static EventResponseDto fromEntity(Event event, boolean viewerIsAdmin) {
        return new EventResponseDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getStatus(),
                event.getCoverImageUrl(),
                event.getEventType(),
                visibleContestUrl(event, viewerIsAdmin),
                event.isShowContestLink(),
                event.isShowWinners(),
                event.isShowAttendeeCount(),
                event.getCreatedBy() != null ? event.getCreatedBy().getName() : null,
                event.getCreatedAt()
        );
    }

    /**
     * The contest link, if this viewer may have it.
     *
     * <p>Withheld rather than hidden in the browser. A link sent and then not
     * rendered is still in the page source, and the entire point of the switch is
     * that the round is not open yet.</p>
     */
    static String visibleContestUrl(Event event, boolean viewerIsAdmin) {
        return viewerIsAdmin || event.isShowContestLink() ? event.getCodeforcesContestUrl() : null;
    }
}
