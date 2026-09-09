package com.cpclub.backend.event;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventPhotoRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The attendance guards.
 *
 * <p>Attendance is the club's official record of who was present, so these tests
 * are mostly about which writes are refused and which are allowed through
 * incomplete.</p>
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventAttendeeRepository eventAttendeeRepository;

    @Mock
    private EventPhotoRepository eventPhotoRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private User admin;
    private User student;
    private Event upcoming;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).name("Admin").email("admin@dau.ac.in").build();
        student = User.builder().id(2L).name("Student").email("student@dau.ac.in").build();
        upcoming = Event.builder()
                .id(10L)
                .title("Weekly Round")
                .eventDate(LocalDateTime.of(2026, 4, 1, 18, 0))
                .location("Lab 1")
                .status(EventStatus.UPCOMING)
                .createdBy(admin)
                .build();
    }

    @Test
    @DisplayName("A member with no phone number can still be recorded as attending")
    void addAttendee_allowsMembersWithoutAPhoneNumber() {
        // The guard that used to reject this was removed deliberately. Every
        // account created before Phase 2 has a null phone number, and only the
        // member can set their own — so blocking here meant an admin standing at
        // an event could add nobody, and had no way to fix it.
        student.setPhoneNumber(null);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventAttendeeRepository.existsByEventIdAndUserId(10L, 2L)).thenReturn(false);
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        when(eventAttendeeRepository.save(any(EventAttendee.class)))
                .thenAnswer(i -> i.getArgument(0));

        EventAttendeeDto result = eventService.addAttendee(10L, 2L, "admin@dau.ac.in");

        assertEquals(2L, result.userId());
        assertFalse(result.hasPhone(), "the admin panel needs to know the number is missing");
        verify(eventAttendeeRepository).save(any(EventAttendee.class));
    }

    @Test
    @DisplayName("hasPhone is true only for a number that is actually usable")
    void addAttendee_treatsABlankPhoneNumberAsMissing() {
        student.setPhoneNumber("   ");
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventAttendeeRepository.existsByEventIdAndUserId(10L, 2L)).thenReturn(false);
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        when(eventAttendeeRepository.save(any(EventAttendee.class)))
                .thenAnswer(i -> i.getArgument(0));

        EventAttendeeDto result = eventService.addAttendee(10L, 2L, "admin@dau.ac.in");

        assertFalse(result.hasPhone());
    }

    @Test
    @DisplayName("Attendance cannot be added to a completed event")
    void addAttendee_rejectsACompletedEvent() {
        upcoming.setStatus(EventStatus.COMPLETED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> eventService.addAttendee(10L, 2L, "admin@dau.ac.in"));

        assertTrue(error.getMessage().contains("completed or cancelled"));
        verify(eventAttendeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Attendance cannot be added to a cancelled event")
    void addAttendee_rejectsACancelledEvent() {
        upcoming.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));

        assertThrows(BadRequestException.class,
                () -> eventService.addAttendee(10L, 2L, "admin@dau.ac.in"));
        verify(eventAttendeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("The status check runs before the member lookup")
    void addAttendee_checksStatusBeforeLookingUpTheMember() {
        // Guard order is the contract: a closed event is the more specific reason
        // the add failed, so a bad user id on a completed event still reports the
        // event, not the user.
        upcoming.setStatus(EventStatus.COMPLETED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));

        assertThrows(BadRequestException.class,
                () -> eventService.addAttendee(10L, 999L, "admin@dau.ac.in"));

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("A member cannot be recorded twice for the same event")
    void addAttendee_rejectsADuplicate() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(userRepository.findById(2L)).thenReturn(Optional.of(student));
        when(eventAttendeeRepository.existsByEventIdAndUserId(10L, 2L)).thenReturn(true);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> eventService.addAttendee(10L, 2L, "admin@dau.ac.in"));

        assertTrue(error.getMessage().contains("already registered"));
        verify(eventAttendeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("An unknown event is a 404, not an empty attendance list")
    void addAttendee_rejectsAnUnknownEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.addAttendee(99L, 2L, "admin@dau.ac.in"));
    }

    @Test
    @DisplayName("An unknown member is a 404 naming the student")
    void addAttendee_rejectsAnUnknownMember() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        ResourceNotFoundException error = assertThrows(ResourceNotFoundException.class,
                () -> eventService.addAttendee(10L, 404L, "admin@dau.ac.in"));

        assertTrue(error.getMessage().contains("Student"));
    }

    @Test
    @DisplayName("Listing attendees of an unknown event is a 404, not an empty list")
    void getAttendees_rejectsAnUnknownEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.getAttendees(99L));
        verify(eventAttendeeRepository, never()).findByEventIdOrderByAddedAtAsc(any());
    }

    @Test
    @DisplayName("The headcount is counted in the database, not by loading every attendee")
    void getEventDetail_countsWithoutLoadingTheAttendanceList() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventPhotoRepository.findByEventIdOrderByUploadedAtAsc(10L)).thenReturn(java.util.List.of());
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(42L);

        assertEquals(42, eventService.getEventDetail(10L).attendeeCount());

        // Loading the list here would pull every attendee's phone number and email
        // out of the database to render a single number on a public page.
        verify(eventAttendeeRepository, never()).findByEventIdOrderByAddedAtAsc(any());
    }

    @Test
    @DisplayName("Updating an event does not change its status")
    void updateEvent_leavesStatusAlone() {
        upcoming.setStatus(EventStatus.COMPLETED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        var result = eventService.updateEvent(10L, new com.cpclub.backend.event.dto.EventCreateRequest(
                "Renamed", "New description",
                LocalDateTime.of(2026, 5, 1, 18, 0), "Lab 2", null));

        assertEquals("Renamed", result.title());
        assertEquals(EventStatus.COMPLETED, result.status(),
                "an ordinary edit must not reopen a completed event");
    }
}
