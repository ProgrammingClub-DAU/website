package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.entity.EventType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One event's full public page.
 *
 * <p>Three things are gated independently, each by its own switch, because the
 * club announces them at different moments: the contest link often goes out as
 * the round opens, the winners cannot exist until it closes, and the turnout
 * figure is sometimes never published. Until a switch is on, the field is absent
 * -- not sent and hidden, but absent.</p>
 *
 * <p>Title, date, location, description and photos are never gated. That is what
 * an event is before it has results, and it is what a workshop stays.</p>
 *
 * @param winners the podium, empty when unpublished or when nobody was recorded
 * @param attendeeCount how many attended, or null when unpublished
 */
public record EventDetailDto(
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
        LocalDateTime createdAt,
        List<EventPhotoDto> photos,
        List<EventWinnerDto> winners,
        Integer attendeeCount
) {

    /**
     * Builds the detail view.
     *
     * @param event the event, with its winners loaded
     * @param photos the event's gallery
     * @param attendeeCount the real attendance figure
     * @param viewerIsAdmin whether the caller holds ROLE_ADMIN, who sees
     *                      everything regardless of the switches
     */
    public static EventDetailDto of(
            Event event,
            List<EventPhotoDto> photos,
            Integer attendeeCount,
            boolean viewerIsAdmin) {

        List<EventWinnerDto> podium = viewerIsAdmin || event.isShowWinners()
                ? event.getWinners().stream().map(EventWinnerDto::fromEntity).toList()
                : List.of();

        Integer visibleCount = viewerIsAdmin || event.isShowAttendeeCount() ? attendeeCount : null;

        return new EventDetailDto(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEventDate(),
                event.getLocation(),
                event.getStatus(),
                event.getCoverImageUrl(),
                event.getEventType(),
                EventResponseDto.visibleContestUrl(event, viewerIsAdmin),
                event.isShowContestLink(),
                event.isShowWinners(),
                event.isShowAttendeeCount(),
                event.getCreatedBy() != null ? event.getCreatedBy().getName() : null,
                event.getCreatedAt(),
                photos,
                podium,
                visibleCount
        );
    }
}
