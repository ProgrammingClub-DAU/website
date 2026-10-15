package com.cpclub.backend.event;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.event.dto.MarkAttendedResultDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventRegistrationRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventRegistrationService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RSVP rules that need no database. The race on the last seat is in
 * {@link EventRegistrationIntegrationTest}, against real PostgreSQL.
 */
class EventRegistrationServiceTest {

    /** 2026-10-15 18:00 IST. */
    private static final Clock IST_CLOCK =
            Clock.fixed(Instant.parse("2026-10-15T12:30:00Z"), ZoneId.of("Asia/Kolkata"));

    private EventRepository eventRepository;
    private EventRegistrationRepository registrationRepository;
    private EventAttendeeRepository attendeeRepository;
    private UserRepository userRepository;

    private final User admin = User.builder().id(1L).name("Admin").email("admin@dau.ac.in").build();
    private final User member = User.builder().id(2L).name("Member").email("m@dau.ac.in").build();

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(EventRegistrationRepository.class);
        attendeeRepository = mock(EventAttendeeRepository.class);
        userRepository = mock(UserRepository.class);
    }

    private EventRegistrationService service(boolean requireVerification) {
        return new EventRegistrationService(eventRepository, registrationRepository, attendeeRepository,
                userRepository, new VerifiedMemberGuard(requireVerification), IST_CLOCK);
    }

    @Test
    @DisplayName("An unverified member cannot RSVP when verification is required")
    void unverified_is403() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(member)); // emailVerifiedAt unset

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service(true).register(10L, 2L));

        assertEquals(403, e.getStatusCode().value());
        assertEquals("EMAIL_NOT_VERIFIED", e.getReason());
        verify(registrationRepository, never()).save(any());
    }

    @Test
    @DisplayName("event_date is IST wall-clock: an event at 17:30 IST has started at 18:00 IST")
    void startTimeIsJudgedInIst() {
        // Read as UTC, 17:30 would still be in the future -- that is the bug this guards.
        Event event = event(EventStatus.UPCOMING, LocalDateTime.of(2026, 10, 15, 17, 30));
        when(userRepository.findById(2L)).thenReturn(Optional.of(member));
        when(eventRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(event));

        ConflictException e = assertThrows(ConflictException.class, () -> service(false).register(10L, 2L));
        assertEquals("Registration closed", e.getMessage());
    }

    @Test
    @DisplayName("An RSVP cannot be cancelled once the event has started")
    void unregisterAfterStart_is409() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event(EventStatus.UPCOMING, LocalDateTime.of(2026, 10, 15, 17, 0))));

        assertThrows(ConflictException.class, () -> service(false).unregister(10L, 2L));
        verify(registrationRepository, never()).deleteByEventIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("Marking three members attended, one already attending, adds two")
    void markAttended_skipsThoseAlreadyAttending() {
        Event event = event(EventStatus.UPCOMING, LocalDateTime.of(2026, 10, 20, 18, 0));
        User a = User.builder().id(3L).name("A").email("a@dau.ac.in").build();
        User b = User.builder().id(4L).name("B").email("b@dau.ac.in").build();
        User c = User.builder().id(5L).name("C").email("c@dau.ac.in").build();
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(a, b, c));
        when(attendeeRepository.findByEventIdOrderByAddedAtAsc(10L))
                .thenReturn(List.of(EventAttendee.builder().event(event).user(b).addedBy(admin).build()));

        MarkAttendedResultDto result = service(false).markAttended(10L, List.of(3L, 4L, 5L), "admin@dau.ac.in");

        assertEquals(2, result.added());
        assertEquals(1, result.skipped());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EventAttendee>> saved = ArgumentCaptor.forClass(List.class);
        verify(attendeeRepository).saveAll(saved.capture());
        assertEquals(List.of(3L, 5L), saved.getValue().stream().map(x -> x.getUser().getId()).toList());
    }

    @Test
    @DisplayName("Attendance follows the Phase 2 rule: not on a completed event")
    void markAttended_onCompletedEvent_is400() {
        when(eventRepository.findById(10L))
                .thenReturn(Optional.of(event(EventStatus.COMPLETED, LocalDateTime.of(2026, 10, 1, 18, 0))));

        assertThrows(BadRequestException.class,
                () -> service(false).markAttended(10L, List.of(3L), "admin@dau.ac.in"));
        verify(attendeeRepository, never()).saveAll(any());
    }

    private static Event event(EventStatus status, LocalDateTime when) {
        return Event.builder().id(10L).title("Round").eventDate(when).location("Lab").status(status).build();
    }
}
