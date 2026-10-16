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
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalendarService {

    /**
     * The zone {@code events.event_date} is stored in.
     *
     * <p>That column predates D31's all-UTC rule: the admin form sends the
     * datetime-local value an admin typed, in IST, and that wall-clock time is what is
     * stored. External contest start times are UTC. Every item this service returns is
     * UTC, so club events are converted on the way out -- and the requested range,
     * which is UTC, is converted on the way in before it is compared with event_date.
     *
     * <p>Before this, events went out unconverted beside UTC contests: club.ics put
     * every club event five and a half hours late in subscribers' calendars, and the
     * merged list sorted the two kinds against each other incorrectly.</p>
     */
    static final ZoneId EVENT_ZONE = ZoneId.of("Asia/Kolkata");

    private final EventRepository eventRepository;
    private final ExternalContestRepository externalContestRepository;

    public List<CalendarItemDto> getRange(LocalDateTime from, LocalDateTime to, List<ContestPlatform> platforms) {
        if (ChronoUnit.DAYS.between(from, to) > 62) {
            throw new IllegalArgumentException("Date range cannot exceed 62 days");
        }

        List<CalendarItemDto> items = new ArrayList<>();

        List<Event> events = eventRepository.findByEventDateBetweenAndStatusIn(
                utcToEventZone(from), utcToEventZone(to), List.of(EventStatus.UPCOMING, EventStatus.COMPLETED));

        for (Event event : events) {
            items.add(CalendarItemDto.builder()
                    .id(event.getId().toString())
                    .type("CLUB_EVENT")
                    .title(event.getTitle())
                    .url("/events/" + event.getId())
                    .startsAt(eventZoneToUtc(event.getEventDate()))
                    // Events record a start only; two hours is the club's usual session.
                    .endsAt(eventZoneToUtc(event.getEventDate()).plusHours(2))
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

    static LocalDateTime eventZoneToUtc(LocalDateTime eventZoneTime) {
        return eventZoneTime.atZone(EVENT_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    static LocalDateTime utcToEventZone(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(EVENT_ZONE).toLocalDateTime();
    }
}
