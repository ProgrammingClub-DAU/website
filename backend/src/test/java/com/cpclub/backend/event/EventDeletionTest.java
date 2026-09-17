package com.cpclub.backend.event;

import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.entity.EventWinner;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventPhotoRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventService;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Deleting an event: allowed, but never by accident.
 */
@ExtendWith(MockitoExtension.class)
class EventDeletionTest {

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

    private Event event;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(10L)
                .title("Spring Sprint")
                .eventDate(LocalDateTime.of(2026, 3, 2, 15, 0))
                .location("Lab 101")
                .status(EventStatus.COMPLETED)
                .build();
    }

    @Test
    @DisplayName("An event with attendance is refused, and the refusal says what would be lost")
    void refusesAnEventWithRecords() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(12L);
        when(eventPhotoRepository.countByEventId(10L)).thenReturn(4L);

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> eventService.deleteEvent(10L, false));

        assertEquals(
                "This event has 12 attendees and 4 photos. Deleting the event removes them permanently. "
                        + "Cancel it instead to keep the record, or confirm the deletion.",
                thrown.getMessage());
        verify(eventRepository, never()).delete(any());
        verify(eventAttendeeRepository, never()).deleteAllByEventId(anyLong());
    }

    @Test
    @DisplayName("Winners alone are enough to require confirmation")
    void winnersAloneRequireConfirmation() {
        event.getWinners().add(EventWinner.builder().event(event).user(member()).position(1).build());
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(0L);
        when(eventPhotoRepository.countByEventId(10L)).thenReturn(0L);

        ConflictException thrown = assertThrows(ConflictException.class,
                () -> eventService.deleteEvent(10L, false));

        assertEquals(true, thrown.getMessage().startsWith("This event has 1 winner."));
    }

    @Test
    @DisplayName("Confirmed, an event is deleted after its attendance and photos, in that order")
    void forceDeletesChildrenFirst() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(12L);
        when(eventPhotoRepository.countByEventId(10L)).thenReturn(4L);

        eventService.deleteEvent(10L, true);

        // Children first: the attendance and photo rows reference the event.
        InOrder order = inOrder(eventAttendeeRepository, eventPhotoRepository, eventRepository);
        order.verify(eventAttendeeRepository).deleteAllByEventId(10L);
        order.verify(eventPhotoRepository).deleteAllByEventId(10L);
        order.verify(eventRepository).delete(event);
    }

    @Test
    @DisplayName("An event with nothing recorded against it deletes without confirmation")
    void emptyEventDeletesDirectly() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(eventAttendeeRepository.countByEventId(10L)).thenReturn(0L);
        when(eventPhotoRepository.countByEventId(10L)).thenReturn(0L);

        eventService.deleteEvent(10L, false);

        verify(eventRepository).delete(event);
    }

    @Test
    @DisplayName("Deleting an event that does not exist is a 404")
    void unknownEventIsNotFound() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> eventService.deleteEvent(99L, true));
        verify(eventRepository, never()).delete(any());
    }

    private User member() {
        User user = new User("Ravi", "ravi@dau.ac.in", null, Role.ROLE_USER);
        user.setId(5L);
        return user;
    }
}
