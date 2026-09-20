package com.cpclub.backend.calendar.service;

import com.cpclub.backend.calendar.dto.CalendarItemDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final EventRepository eventRepository;
    private final ExternalContestRepository externalContestRepository;

    public List<CalendarItemDto> getRange(LocalDateTime from, LocalDateTime to, List<ContestPlatform> platforms) {
        if (ChronoUnit.DAYS.between(from, to) > 62) {
            throw new IllegalArgumentException("Date range cannot exceed 62 days");
        }

        List<CalendarItemDto> items = new ArrayList<>();

        List<Event> events = eventRepository.findByEventDateBetweenAndStatusIn(
                from, to, List.of(EventStatus.UPCOMING, EventStatus.COMPLETED));

        for (Event event : events) {
            items.add(CalendarItemDto.builder()
                    .id(event.getId().toString())
                    .type("CLUB_EVENT")
                    .title(event.getTitle())
                    .url("/events/" + event.getId())
                    .startsAt(event.getEventDate())
                    .endsAt(event.getEventDate().plusHours(2)) // Default duration for club events?
                    .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                    .build());
        }

        List<ExternalContest> contests;
        if (platforms == null || platforms.isEmpty()) {
            contests = externalContestRepository.findByStartsAtGreaterThanEqualAndStartsAtLessThanEqual(from, to);
        } else {
            contests = externalContestRepository.findByPlatformInAndStartsAtGreaterThanEqualAndStartsAtLessThanEqual(platforms, from, to);
        }

        for (ExternalContest contest : contests) {
            items.add(CalendarItemDto.builder()
                    .id(contest.getExternalId())
                    .type("EXTERNAL_CONTEST")
                    .platform(contest.getPlatform().name())
                    .title(contest.getName())
                    .url(contest.getUrl())
                    .startsAt(contest.getStartsAt())
                    .endsAt(contest.getStartsAt().plusSeconds(contest.getDurationSeconds()))
                    .build());
        }

        items.sort((a, b) -> a.getStartsAt().compareTo(b.getStartsAt()));
        return items;
    }
}
