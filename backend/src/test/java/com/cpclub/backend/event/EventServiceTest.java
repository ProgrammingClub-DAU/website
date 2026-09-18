package com.cpclub.backend.event;

import org.springframework.context.ApplicationEventPublisher;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.dto.AddEventPhotoRequest;
import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.dto.EventCreateRequest;
import com.cpclub.backend.event.dto.EventPhotoDto;
import com.cpclub.backend.event.dto.EventResponseDto;
import com.cpclub.backend.event.dto.SetEventWinnersRequest;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventPhoto;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.livesheet.AttendanceChangedEvent;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
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

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        // The live sheet hears about it; the listener only acts after commit.
        verify(eventPublisher).publishEvent(new AttendanceChangedEvent(10L));
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

        assertEquals(42, eventService.getEventDetail(10L, true).attendeeCount());

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
                LocalDateTime.of(2026, 5, 1, 18, 0), "Lab 2", null,
                null, null, false, false, false));

        assertEquals("Renamed", result.title());
        assertEquals(EventStatus.COMPLETED, result.status(),
                "an ordinary edit must not reopen a completed event");
    }

    // -- Event lifecycle -----------------------------------------------------

    @Test
    @DisplayName("A new event starts UPCOMING and records the admin who created it")
    void createEvent_success() {
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponseDto result = eventService.createEvent(new EventCreateRequest(
                "Spring Code Sprint", "Five problems, two hours",
                LocalDateTime.of(2026, 10, 3, 15, 0), "Lab 101", null,
                null, null, false, false, false), "admin@dau.ac.in");

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertEquals(EventStatus.UPCOMING, saved.getValue().getStatus(),
                "a request cannot create an event that is already completed or cancelled");
        assertEquals(admin, saved.getValue().getCreatedBy());
        assertEquals("Spring Code Sprint", result.title());
        assertEquals(EventStatus.UPCOMING, result.status());
    }

    @Test
    @DisplayName("Creating an event as an account that no longer exists is a 404")
    void createEvent_rejectsAnUnknownAdmin() {
        when(userRepository.findByEmail("gone@dau.ac.in")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.createEvent(new EventCreateRequest(
                "Title", null, LocalDateTime.of(2026, 10, 3, 15, 0), "Lab 101", null,
                null, null, false, false, false), "gone@dau.ac.in"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Marking an event completed sets its status")
    void markCompleted_success() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponseDto result = eventService.markEventCompleted(10L);

        assertEquals(EventStatus.COMPLETED, result.status());
        assertEquals(EventStatus.COMPLETED, upcoming.getStatus());
    }

    @Test
    @DisplayName("Marking an unknown event completed is a 404")
    void markCompleted_rejectsAnUnknownEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.markEventCompleted(99L));
        verify(eventRepository, never()).save(any());
    }

    // -- Attendance removal --------------------------------------------------

    @Test
    @DisplayName("Removing an attendee deletes exactly that attendance row")
    void removeAttendee_success() {
        EventAttendee row = EventAttendee.builder().id(5L).event(upcoming).user(student).addedBy(admin).build();
        when(eventAttendeeRepository.findByEventIdAndUserId(10L, 2L)).thenReturn(Optional.of(row));

        eventService.removeAttendee(10L, 2L);

        verify(eventAttendeeRepository).delete(row);
        verify(eventPublisher).publishEvent(new AttendanceChangedEvent(10L));
    }

    @Test
    @DisplayName("Removing a member who was never recorded is a 404, not a silent no-op")
    void removeAttendee_rejectsAMissingRow() {
        when(eventAttendeeRepository.findByEventIdAndUserId(10L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.removeAttendee(10L, 2L));
        verify(eventAttendeeRepository, never()).delete(any());
        // Nothing changed, so the sheet is not touched.
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    // -- Event photos --------------------------------------------------------

    @Test
    @DisplayName("An event photo stores the Cloudinary URL, caption and uploader")
    void addEventPhoto_success() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        when(eventPhotoRepository.save(any(EventPhoto.class))).thenAnswer(i -> i.getArgument(0));

        EventPhotoDto result = eventService.addEventPhoto(10L,
                new AddEventPhotoRequest("https://res.cloudinary.com/demo/image/upload/round.jpg", "Final standings"),
                "admin@dau.ac.in");

        ArgumentCaptor<EventPhoto> saved = ArgumentCaptor.forClass(EventPhoto.class);
        verify(eventPhotoRepository).save(saved.capture());
        assertEquals(upcoming, saved.getValue().getEvent());
        assertEquals(admin, saved.getValue().getUploadedBy());
        assertEquals("https://res.cloudinary.com/demo/image/upload/round.jpg", result.imageUrl());
        assertEquals("Final standings", result.caption());
    }

    @Test
    @DisplayName("A photo cannot be attached to an unknown event")
    void addEventPhoto_rejectsAnUnknownEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.addEventPhoto(99L,
                new AddEventPhotoRequest("https://res.cloudinary.com/demo/image/upload/x.jpg", null),
                "admin@dau.ac.in"));
        verify(eventPhotoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deleting an existing photo removes that row")
    void deleteEventPhoto_success() {
        EventPhoto photo = EventPhoto.builder().id(7L).event(upcoming)
                .imageUrl("https://res.cloudinary.com/demo/image/upload/x.jpg").uploadedBy(admin).build();
        when(eventPhotoRepository.findById(7L)).thenReturn(Optional.of(photo));

        eventService.deleteEventPhoto(7L);

        verify(eventPhotoRepository).delete(photo);
    }

    @Test
    @DisplayName("Deleting a photo that does not exist is a 404")
    void deleteEventPhoto_notFound() {
        when(eventPhotoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.deleteEventPhoto(404L));
        verify(eventPhotoRepository, never()).delete(any());
    }
    // -- Winners -------------------------------------------------------------

    @Test
    @DisplayName("An event cannot have two firsts")
    void setWinners_rejectsARepeatedPlacing() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> eventService.setWinners(10L, new SetEventWinnersRequest(List.of(
                        new SetEventWinnersRequest.Winner(1, 5L),
                        new SetEventWinnersRequest.Winner(1, 6L)))));

        assertTrue(thrown.getMessage().contains("placing"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("One member cannot stand in two places at once")
    void setWinners_rejectsARepeatedMember() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));

        assertThrows(BadRequestException.class,
                () -> eventService.setWinners(10L, new SetEventWinnersRequest(List.of(
                        new SetEventWinnersRequest.Winner(1, 5L),
                        new SetEventWinnersRequest.Winner(2, 5L)))));

        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("A winner who was never marked present is refused, not quietly recorded")
    void setWinners_rejectsSomebodyWhoDidNotAttend() {
        // Nearly always a mis-click on a name that looked right in a dropdown.
        // Cheap to refuse here, expensive to notice on the public page.
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventAttendeeRepository.existsByEventIdAndUserId(10L, 5L)).thenReturn(false);

        BadRequestException thrown = assertThrows(BadRequestException.class,
                () -> eventService.setWinners(10L, new SetEventWinnersRequest(List.of(
                        new SetEventWinnersRequest.Winner(1, 5L)))));

        assertTrue(thrown.getMessage().toLowerCase().contains("attending"));
        verify(eventRepository, never()).save(any());
    }

    @Test
    @DisplayName("Placings are recorded for members who attended")
    void setWinners_recordsThePodium() {
        User winner = new User("Ravi", "ravi@dau.ac.in", null, com.cpclub.backend.user.entity.Role.ROLE_USER);
        winner.setId(5L);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventAttendeeRepository.existsByEventIdAndUserId(10L, 5L)).thenReturn(true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(winner));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(eventPhotoRepository.findByEventIdOrderByUploadedAtAsc(10L)).thenReturn(List.of());
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(1L);

        eventService.setWinners(10L, new SetEventWinnersRequest(List.of(
                new SetEventWinnersRequest.Winner(1, 5L))));

        assertEquals(1, upcoming.getWinners().size());
        assertEquals(5L, upcoming.getWinners().get(0).getUser().getId());
        assertEquals(1, upcoming.getWinners().get(0).getPosition());
    }

    @Test
    @DisplayName("An empty list clears the podium, which is how a mistake is undone")
    void setWinners_clearsThePodium() {
        upcoming.getWinners().add(com.cpclub.backend.event.entity.EventWinner.builder()
                .event(upcoming).user(admin).position(1).build());

        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));
        when(eventPhotoRepository.findByEventIdOrderByUploadedAtAsc(10L)).thenReturn(List.of());
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(0L);

        eventService.setWinners(10L, new SetEventWinnersRequest(List.of()));

        assertTrue(upcoming.getWinners().isEmpty());
    }
    // -- Reopening ------------------------------------------------------------

    @Test
    @DisplayName("A completed event can be put back to upcoming, which unfreezes attendance")
    void reopenEvent_fromCompleted() {
        upcoming.setStatus(EventStatus.COMPLETED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(EventStatus.UPCOMING, eventService.reopenEvent(10L).status());

        // The point of the feature: addAttendee refuses anything not upcoming,
        // so an event completed by mistake was previously uncorrectable.
        assertEquals(EventStatus.UPCOMING, upcoming.getStatus());
    }

    @Test
    @DisplayName("A cancelled event can be reopened too")
    void reopenEvent_fromCancelled() {
        upcoming.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(EventStatus.UPCOMING, eventService.reopenEvent(10L).status());
    }

    @Test
    @DisplayName("Reopening leaves the results alone, including whether they are published")
    void reopenEvent_doesNotTouchResults() {
        // An admin reopening an event to fix its attendance has not asked to
        // un-announce its winners. Clearing settings they would have to rebuild
        // is a worse surprise than leaving them; the admin panel warns instead.
        upcoming.setStatus(EventStatus.COMPLETED);
        upcoming.setCodeforcesContestUrl("https://codeforces.com/contest/1234");
        upcoming.setShowContestLink(true);
        upcoming.setShowWinners(true);
        upcoming.setShowAttendeeCount(true);

        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        eventService.reopenEvent(10L);

        assertEquals("https://codeforces.com/contest/1234", upcoming.getCodeforcesContestUrl());
        assertTrue(upcoming.isShowContestLink());
        assertTrue(upcoming.isShowWinners());
        assertTrue(upcoming.isShowAttendeeCount());
    }

    @Test
    @DisplayName("Reopening an event that is already upcoming succeeds rather than erroring")
    void reopenEvent_isIdempotent() {
        // Two admins pressing at once should not produce a failure.
        when(eventRepository.findById(10L)).thenReturn(Optional.of(upcoming));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(EventStatus.UPCOMING, eventService.reopenEvent(10L).status());
    }

    @Test
    @DisplayName("Reopening an event that does not exist is a 404")
    void reopenEvent_rejectsUnknownEvent() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.reopenEvent(99L));
        verify(eventRepository, never()).save(any());
    }
}
