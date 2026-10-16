package com.cpclub.backend.mail;

import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventRegistration;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRegistrationRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.mail.repository.ReminderLogRepository;
import com.cpclub.backend.mail.service.MailService;
import com.cpclub.backend.mail.service.ReminderService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reminder emails against real PostgreSQL: the idempotency gate is a native
 * {@code INSERT ... ON CONFLICT DO NOTHING} on reminder_log's unique constraint.
 *
 * <p>The clock is fixed at 2026-10-15 10:00 IST, so the hourly run's window is
 * (16 Oct 09:00, 16 Oct 10:00] IST and the digest looks at 15 Oct 04:30 UTC
 * onwards.</p>
 */
@Transactional
@Import(ReminderServiceIntegrationTest.FixedClock.class)
@TestPropertySource(properties = {
        "cpclub.mail.unsubscribe-secret=0123456789abcdef0123456789abcdef",
        "cpclub.public-site-url=https://club.example",
        "cpclub.public-api-url=https://api.club.example/"
})
class ReminderServiceIntegrationTest extends PostgresIntegrationTest {

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedTestClock() {
            return Clock.fixed(Instant.parse("2026-10-15T04:30:00Z"), ZoneId.of("Asia/Kolkata"));
        }
    }

    @MockitoBean
    private MailService mailService;

    @Autowired private ReminderService reminderService;
    @Autowired private EventRepository eventRepository;
    @Autowired private EventRegistrationRepository registrationRepository;
    @Autowired private ExternalContestRepository externalContestRepository;
    @Autowired private ReminderLogRepository reminderLogRepository;
    @Autowired private UserRepository userRepository;

    private User eventsFan;
    private User rsvpd;
    private User contestsOnly;
    private User admin;

    @BeforeEach
    void setUp() {
        when(mailService.isEnabled()).thenReturn(true);
        admin = userRepository.save(User.builder().name("Admin").email("admin-rem@dau.ac.in").build());
        eventsFan = userRepository.save(User.builder().name("Fan").email("fan-rem@dau.ac.in")
                .notifyEvents(true).notifyContests(true).build());
        rsvpd = userRepository.save(User.builder().name("Rsvpd").email("rsvpd-rem@dau.ac.in")
                .notifyEvents(true).build());
        contestsOnly = userRepository.save(User.builder().name("Contests").email("contests-rem@dau.ac.in")
                .notifyContests(true).build());
    }

    @Test
    @DisplayName("The hourly job run twice in the same window sends one email per member")
    void hourlyJobTwice_oneEmailEach() {
        event("Weekly Round", LocalDateTime.of(2026, 10, 16, 9, 30));

        assertEquals(2, reminderService.sendEventReminders());
        assertEquals(0, reminderService.sendEventReminders(), "the second run finds every reminder claimed");

        verify(mailService, times(1)).send(eq("fan-rem@dau.ac.in"), anyString(), anyString(), anyString(), anyMap());
        verify(mailService, times(1)).send(eq("rsvpd-rem@dau.ac.in"), anyString(), anyString(), anyString(), anyMap());
        verify(mailService, never()).send(eq("contests-rem@dau.ac.in"), any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("A member who RSVP'd is told so")
    void registeredMemberIsToldSo() {
        Event event = event("Weekly Round", LocalDateTime.of(2026, 10, 16, 9, 30));
        registrationRepository.save(EventRegistration.builder().event(event).user(rsvpd).build());

        reminderService.sendEventReminders();

        assertTrue(textSentTo("rsvpd-rem@dau.ac.in").contains("You're registered."));
        assertFalse(textSentTo("fan-rem@dau.ac.in").contains("You're registered."));
    }

    @Test
    @DisplayName("Only events in (now + 23h, now + 24h] are reminded")
    void onlyTheWindow() {
        event("Too soon", LocalDateTime.of(2026, 10, 16, 9, 0));   // exactly now + 23h: excluded
        event("Too late", LocalDateTime.of(2026, 10, 16, 10, 1));
        event("Cancelled", LocalDateTime.of(2026, 10, 16, 9, 30), EventStatus.CANCELLED);

        assertEquals(0, reminderService.sendEventReminders());
    }

    @Test
    @DisplayName("Every email carries one-click unsubscribe headers pointing at the API")
    void unsubscribeHeaders() {
        event("Weekly Round", LocalDateTime.of(2026, 10, 16, 9, 30));

        reminderService.sendEventReminders();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(mailService).send(eq("fan-rem@dau.ac.in"), any(), any(), any(), headers.capture());
        assertEquals("List-Unsubscribe=One-Click", headers.getValue().get("List-Unsubscribe-Post"));
        // The API, not the site: the mail provider POSTs here directly (RFC 8058).
        assertTrue(headers.getValue().get("List-Unsubscribe")
                .startsWith("<https://api.club.example/api/notifications/unsubscribe?token="),
                headers.getValue().get("List-Unsubscribe"));
        // The link in the body goes to the site's page.
        assertTrue(textSentTo("fan-rem@dau.ac.in").contains("https://club.example/unsubscribe?token="));
    }

    @Test
    @DisplayName("No contests in the next 24 hours: no digest at all")
    void digestWithNothing_sendsNothing() {
        assertEquals(0, reminderService.sendContestDigest());
        verify(mailService, never()).send(any(), any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("The digest goes only to members with notify_contests on")
    void digestRespectsThePreference() {
        externalContestRepository.save(ExternalContest.builder()
                .platform(ContestPlatform.CODEFORCES).externalId("2001").name("Codeforces Round 999")
                .url("https://codeforces.com/contests/2001")
                .startsAt(LocalDateTime.of(2026, 10, 15, 14, 35)).durationSeconds(7200)
                .syncedAt(LocalDateTime.of(2026, 10, 15, 0, 0)).build());

        assertEquals(2, reminderService.sendContestDigest());
        assertEquals(0, reminderService.sendContestDigest(), "once a day");

        verify(mailService).send(eq("contests-rem@dau.ac.in"), any(), any(), any(), anyMap());
        verify(mailService).send(eq("fan-rem@dau.ac.in"), any(), any(), any(), anyMap());
        verify(mailService, never()).send(eq("rsvpd-rem@dau.ac.in"), any(), any(), any(), anyMap());
        // 14:35 UTC is 20:05 IST.
        assertTrue(textSentTo("contests-rem@dau.ac.in").contains("8:05 PM IST"));
    }

    @Test
    @DisplayName("With mail disabled nothing is claimed, so turning mail on later loses nothing")
    void mailDisabled_claimsNothing() {
        when(mailService.isEnabled()).thenReturn(false);
        event("Weekly Round", LocalDateTime.of(2026, 10, 16, 9, 30));

        assertEquals(0, reminderService.sendEventReminders());
        assertEquals(0, reminderLogRepository.count());
    }

    private Event event(String title, LocalDateTime istWallClock) {
        return event(title, istWallClock, EventStatus.UPCOMING);
    }

    private Event event(String title, LocalDateTime istWallClock, EventStatus status) {
        return eventRepository.save(Event.builder().title(title).eventDate(istWallClock)
                .location("Lab 1").status(status).createdBy(admin).build());
    }

    private String textSentTo(String email) {
        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(mailService).send(eq(email), anyString(), text.capture(), anyString(), anyMap());
        return text.getValue();
    }
}
