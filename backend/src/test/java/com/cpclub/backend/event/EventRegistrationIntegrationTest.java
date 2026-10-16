package com.cpclub.backend.event;

import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.event.dto.RegistrationStatusDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRegistrationRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventRegistrationService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RSVPs against real PostgreSQL.
 *
 * <p>Deliberately <em>not</em> {@code @Transactional}: the race test needs two real,
 * separately committing transactions to contend for the event's row lock, and a
 * test-managed transaction would put both on one connection. Rows are cleaned up
 * by hand instead.</p>
 */
class EventRegistrationIntegrationTest extends PostgresIntegrationTest {

    @Autowired private EventRegistrationService registrationService;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository registrationRepository;
    @Autowired private UserRepository userRepository;

    private User admin;
    private final List<Long> users = new ArrayList<>();
    private final List<Long> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        admin = save(User.builder().name("Admin").email("admin-rsvp@dau.ac.in").build());
    }

    @AfterEach
    void cleanUp() {
        for (Long eventId : events) {
            registrationRepository.findByEventIdWithUser(eventId).forEach(registrationRepository::delete);
            eventRepository.deleteById(eventId);
        }
        userRepository.deleteAllById(users);
        users.clear();
        events.clear();
    }

    @Test
    @DisplayName("Capacity 1, two registrations at once: exactly one succeeds, the other gets 409")
    void lastSeat_onlyOneWinner() throws Exception {
        Event event = event(EventStatus.UPCOMING, LocalDateTime.now().plusDays(3), 1);
        User a = save(User.builder().name("A").email("a-rsvp@dau.ac.in").build());
        User b = save(User.builder().name("B").email("b-rsvp@dau.ac.in").build());

        // Both threads are released together, so both reach the row lock at once.
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<RegistrationStatusDto>> results = new ArrayList<>();
            for (User u : List.of(a, b)) {
                Callable<RegistrationStatusDto> task = () -> {
                    start.await();
                    return registrationService.register(event.getId(), u.getId());
                };
                results.add(pool.submit(task));
            }
            start.countDown();

            int succeeded = 0;
            int full = 0;
            for (Future<RegistrationStatusDto> f : results) {
                try {
                    f.get(30, TimeUnit.SECONDS);
                    succeeded++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertTrue(e.getCause() instanceof ConflictException, "unexpected: " + e.getCause());
                    assertEquals("Event is full", e.getCause().getMessage());
                    full++;
                }
            }

            assertEquals(1, succeeded);
            assertEquals(1, full);
            assertEquals(1, registrationRepository.countByEventId(event.getId()), "never over capacity");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Registering for a completed event is 409 Registration closed")
    void completedEvent_isClosed() {
        Event event = event(EventStatus.COMPLETED, LocalDateTime.now().plusDays(1), null);
        User a = save(User.builder().name("A").email("a2-rsvp@dau.ac.in").build());

        ConflictException e = assertThrows(ConflictException.class,
                () -> registrationService.register(event.getId(), a.getId()));
        assertEquals("Registration closed", e.getMessage());
    }

    @Test
    @DisplayName("An event that has already started takes no more RSVPs")
    void startedEvent_isClosed() {
        // event_date is IST wall-clock. One hour ago, in IST, has started whatever
        // zone the test JVM runs in.
        LocalDateTime anHourAgoIst = LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).minusHours(1);
        Event event = event(EventStatus.UPCOMING, anHourAgoIst, null);
        User a = save(User.builder().name("A").email("a3-rsvp@dau.ac.in").build());

        assertThrows(ConflictException.class, () -> registrationService.register(event.getId(), a.getId()));
    }

    @Test
    @DisplayName("Registering twice leaves one row and succeeds both times")
    void registerTwice_isIdempotent() {
        Event event = event(EventStatus.UPCOMING, LocalDateTime.now().plusDays(2), 5);
        User a = save(User.builder().name("A").email("a4-rsvp@dau.ac.in").build());

        RegistrationStatusDto first = registrationService.register(event.getId(), a.getId());
        RegistrationStatusDto second = registrationService.register(event.getId(), a.getId());

        assertTrue(first.registered());
        assertTrue(second.registered());
        assertEquals(1, second.count());
        assertEquals(1, registrationRepository.countByEventId(event.getId()));
    }

    @Test
    @DisplayName("A registered member re-registering on a now-full event is not told it is full")
    void registeredMember_onFullEvent_isStillOk() {
        Event event = event(EventStatus.UPCOMING, LocalDateTime.now().plusDays(2), 1);
        User a = save(User.builder().name("A").email("a5-rsvp@dau.ac.in").build());
        registrationService.register(event.getId(), a.getId());

        RegistrationStatusDto again = registrationService.register(event.getId(), a.getId());

        assertTrue(again.registered());
    }

    @Test
    @DisplayName("Cancelling an RSVP is idempotent")
    void unregister_isIdempotent() {
        Event event = event(EventStatus.UPCOMING, LocalDateTime.now().plusDays(2), null);
        User a = save(User.builder().name("A").email("a6-rsvp@dau.ac.in").build());
        registrationService.register(event.getId(), a.getId());

        registrationService.unregister(event.getId(), a.getId());
        registrationService.unregister(event.getId(), a.getId());

        assertEquals(0, registrationRepository.countByEventId(event.getId()));
    }

    private User save(User user) {
        User saved = userRepository.save(user);
        users.add(saved.getId());
        return saved;
    }

    private Event event(EventStatus status, LocalDateTime when, Integer capacity) {
        Event saved = eventRepository.save(Event.builder()
                .title("RSVP test")
                .eventDate(when)
                .location("Lab 1")
                .status(status)
                .capacity(capacity)
                .createdBy(admin)
                .build());
        events.add(saved.getId());
        return saved;
    }
}
