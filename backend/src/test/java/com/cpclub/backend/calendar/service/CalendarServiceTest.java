package com.cpclub.backend.calendar.service;

import com.cpclub.backend.calendar.dto.CalendarItemDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Club events are stored in IST wall-clock; everything the calendar returns is UTC.
 * These pin both directions of that conversion, because the ICS feed and the
 * merged list are only right if both are.
 */
class CalendarServiceTest {

    private final EventRepository eventRepository = mock(EventRepository.class);
    private final ExternalContestRepository contestRepository = mock(ExternalContestRepository.class);
    private final CalendarService service = new CalendarService(eventRepository, contestRepository);

    @Test
    @DisplayName("An 18:00 IST club event comes out at 12:30 UTC")
    void clubEventIsReturnedInUtc() {
        Event event = Event.builder().id(7L).title("Weekly Round").location("Lab 1")
                .eventDate(LocalDateTime.of(2026, 10, 20, 18, 0)).status(EventStatus.UPCOMING).build();
        when(eventRepository.findByEventDateBetweenAndStatusIn(any(), any(), anyList())).thenReturn(List.of(event));

        CalendarItemDto item = service.getRange(
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 0, 0), null).get(0);

        assertEquals(LocalDateTime.of(2026, 10, 20, 12, 30), item.getStartsAt());
        assertEquals(LocalDateTime.of(2026, 10, 20, 14, 30), item.getEndsAt());
    }

    @Test
    @DisplayName("The requested UTC range is moved into IST before it is compared with event_date")
    void rangeIsConvertedBeforeQuerying() {
        service.getRange(LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0), null);

        verify(eventRepository).findByEventDateBetweenAndStatusIn(
                eq(LocalDateTime.of(2026, 10, 20, 5, 30)), eq(LocalDateTime.of(2026, 10, 21, 5, 30)), anyList());
    }

    @Test
    @DisplayName("Club events and contests sort together on one clock")
    void mixedItemsSortCorrectly() {
        // 15:00 IST is 09:30 UTC -- before the 10:00 UTC contest. Compared as raw
        // values, 15:00 would wrongly sort after it.
        Event event = Event.builder().id(7L).title("Lecture").location("Lab 1")
                .eventDate(LocalDateTime.of(2026, 10, 20, 15, 0)).status(EventStatus.UPCOMING).build();
        ExternalContest contest = ExternalContest.builder().platform(ContestPlatform.CODEFORCES).externalId("2001")
                .name("Codeforces Round").url("https://codeforces.com/contests/2001")
                .startsAt(LocalDateTime.of(2026, 10, 20, 10, 0)).durationSeconds(7200)
                .syncedAt(LocalDateTime.of(2026, 10, 19, 0, 0)).build();
        when(eventRepository.findByEventDateBetweenAndStatusIn(any(), any(), anyList())).thenReturn(List.of(event));
        when(contestRepository.findByStartsAtGreaterThanEqualAndStartsAtLessThanEqual(any(), any()))
                .thenReturn(List.of(contest));

        List<CalendarItemDto> items = service.getRange(
                LocalDateTime.of(2026, 10, 20, 0, 0), LocalDateTime.of(2026, 10, 21, 0, 0), null);

        assertEquals(List.of("CLUB_EVENT", "EXTERNAL_CONTEST"), items.stream().map(CalendarItemDto::getType).toList());
    }

    @Test
    @DisplayName("club.ics carries the event at the right instant")
    void icsGetsTheRightInstant() {
        Event event = Event.builder().id(7L).title("Weekly Round").location("Lab 1")
                .eventDate(LocalDateTime.of(2026, 10, 20, 18, 0)).status(EventStatus.UPCOMING).build();
        when(eventRepository.findByEventDateBetweenAndStatusIn(any(), any(), anyList())).thenReturn(List.of(event));
        CalendarItemDto item = service.getRange(
                LocalDateTime.of(2026, 10, 1, 0, 0), LocalDateTime.of(2026, 10, 31, 0, 0), null).get(0);

        IcsWriter.IcsEvent ics = new IcsWriter.IcsEvent();
        ics.setUid("event-7@club.example");
        ics.setStartUtc(item.getStartsAt());
        ics.setEndUtc(item.getEndsAt());
        ics.setSummary(item.getTitle());
        String feed = new IcsWriter().write(List.of(ics));

        assertTrue(feed.contains("DTSTART:20261020T123000Z"), feed);
    }
}
