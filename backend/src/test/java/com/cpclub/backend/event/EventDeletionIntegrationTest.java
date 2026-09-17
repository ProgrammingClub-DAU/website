package com.cpclub.backend.event;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventPhoto;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.entity.EventWinner;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventPhotoRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventService;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deleting an event against a real database.
 *
 * <p>The unit test proves the calls happen in order; only a database proves the
 * order is enough. Attendance, photos and winners all hold foreign keys to the
 * event, and this schema -- generated from the entities -- has no ON DELETE
 * CASCADE to fall back on. If any child were left behind, the event delete
 * would fail here with a constraint violation.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventDeletionIntegrationTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventAttendeeRepository eventAttendeeRepository;

    @Autowired
    private EventPhotoRepository eventPhotoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("A confirmed delete removes the event with its attendance, photos and winners")
    void deletesEverythingRecordedAgainstTheEvent() {
        User admin = userRepository.save(new User("Admin", "admin-del@dau.ac.in", null, Role.ROLE_ADMIN));
        User member = userRepository.save(new User("Ravi", "ravi-del@dau.ac.in", null, Role.ROLE_USER));

        Event event = eventRepository.save(Event.builder()
                .title("Spring Sprint")
                .eventDate(LocalDateTime.of(2026, 3, 2, 15, 0))
                .location("Lab 101")
                .status(EventStatus.COMPLETED)
                .createdBy(admin)
                .build());

        eventAttendeeRepository.save(EventAttendee.builder()
                .event(event).user(member).addedBy(admin).build());
        eventPhotoRepository.save(EventPhoto.builder()
                .event(event).imageUrl("https://res.cloudinary.com/x.jpg").uploadedBy(admin).build());
        event.getWinners().add(EventWinner.builder().event(event).user(member).position(1).build());
        eventRepository.save(event);

        Long id = event.getId();
        entityManager.flush();
        entityManager.clear();

        eventService.deleteEvent(id, true);
        entityManager.flush();
        entityManager.clear();

        assertFalse(eventRepository.existsById(id), "the event is gone");
        assertEquals(0, eventAttendeeRepository.countByEventId(id), "its attendance is gone");
        assertEquals(0, eventPhotoRepository.countByEventId(id), "its photos are gone");
        assertTrue(userRepository.existsById(member.getId()), "the members themselves are untouched");
    }
}
