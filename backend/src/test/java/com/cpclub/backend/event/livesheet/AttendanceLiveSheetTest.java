package com.cpclub.backend.event.livesheet;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventAttendee;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventExportService;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The sync rules, against an in-memory spreadsheet instead of Google.
 */
class AttendanceLiveSheetTest {

    private static final String SPREADSHEET = "sheet-abc";
    private static final String ROBOT = "attendance@cpclub-test.iam.gserviceaccount.com";
    private static final Instant NOW = Instant.parse("2026-09-18T10:00:00Z");

    private EventRepository eventRepository;
    private EventAttendeeRepository attendeeRepository;
    private FakeSheets sheets;
    private ScheduledExecutorService executor;
    private Event event;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        attendeeRepository = mock(EventAttendeeRepository.class);
        sheets = new FakeSheets();
        executor = Executors.newSingleThreadScheduledExecutor();

        event = Event.builder()
                .id(7L)
                .title("IPC 2026")
                .eventDate(LocalDateTime.of(2026, 9, 20, 18, 0))
                .location("Lab 1")
                .status(EventStatus.UPCOMING)
                .build();
        when(eventRepository.existsById(7L)).thenReturn(true);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));
        when(attendeeRepository.findByEventIdOrderByAddedAtAsc(7L)).thenReturn(List.of(
                attendee(1L, "Asha", "202401001@dau.ac.in"),
                attendee(2L, "Ravi", "202401002@dau.ac.in")
        ));
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @DisplayName("The first update creates the event's tab and writes the header and every attendee")
    void firstSyncCreatesTheTab() {
        LiveSheetStatusDto status = liveSheet(enabled()).syncNow(7L);

        assertEquals(1, sheets.tabs.size());
        SheetsApi.SheetTab tab = sheets.tabs.get(0);
        assertEquals("#7 IPC 2026", tab.title());
        List<List<String>> written = sheets.contents.get(tab.sheetId());
        assertEquals(3, written.size(), "header plus two attendees");
        assertEquals("Name", written.get(0).get(0));
        assertEquals("Asha", written.get(1).get(0));
        assertEquals("202401002", written.get(2).get(3), "student ID from the address");

        assertTrue(status.enabled());
        assertNull(status.lastError());
        assertNotNull(status.lastSyncedAt());
        assertTrue(status.sheetUrl().endsWith("/d/" + SPREADSHEET + "/edit#gid=" + tab.sheetId()));
        assertEquals(ROBOT, status.serviceAccountEmail());
    }

    @Test
    @DisplayName("A renamed event keeps its tab: it is found by id and renamed, not duplicated")
    void renamedEventReusesItsTab() {
        AttendanceLiveSheet liveSheet = liveSheet(enabled());
        liveSheet.syncNow(7L);

        event.setTitle("IPC 2026 Finals");
        liveSheet.syncNow(7L);

        assertEquals(1, sheets.tabs.size());
        assertEquals("#7 IPC 2026 Finals", sheets.tabs.get(0).title());
        assertEquals(1, sheets.addCalls, "the tab is created once");
    }

    @Test
    @DisplayName("Event 12 does not claim event 123's tab")
    void findTabMatchesTheWholeId() {
        List<SheetsApi.SheetTab> tabs = List.of(
                new SheetsApi.SheetTab(1, "#123 Other event", 100, 6),
                new SheetsApi.SheetTab(2, "#12 This event", 100, 6)
        );

        assertEquals(2, AttendanceLiveSheet.findTab(tabs, 12L).orElseThrow().sheetId());
        assertTrue(AttendanceLiveSheet.findTab(tabs, 1L).isEmpty());
    }

    @Test
    @DisplayName("Tab names drop the characters Sheets rejects and stay within 100 characters")
    void tabTitleIsSafe() {
        assertEquals("#7 Round 1 Div 2 A B", AttendanceLiveSheet.tabTitle(7L, "Round 1: Div/2 [A*B]?"));
        assertEquals("#7", AttendanceLiveSheet.tabTitle(7L, "  "));
        assertEquals(100, AttendanceLiveSheet.tabTitle(7L, "x".repeat(300)).length());
    }

    @Test
    @DisplayName("A refusal from Google is kept, in words, and does not escape the sync")
    void failureIsRecordedNotThrown() {
        sheets.failWith = HttpClientErrorException.create(HttpStatus.FORBIDDEN, "Forbidden", null,
                "{\"error\":{\"status\":\"PERMISSION_DENIED\"}}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        LiveSheetStatusDto status = liveSheet(enabled()).syncNow(7L);

        assertTrue(status.lastError().contains("Share it with " + ROBOT), status.lastError());
        assertNull(status.lastSyncedAt());
    }

    @Test
    @DisplayName("A success after a failure clears the error")
    void successClearsTheError() {
        AttendanceLiveSheet liveSheet = liveSheet(enabled());
        sheets.failWith = new RuntimeException("boom");
        assertNotNull(liveSheet.syncNow(7L).lastError());

        sheets.failWith = null;
        LiveSheetStatusDto status = liveSheet.syncNow(7L);

        assertNull(status.lastError());
        assertNotNull(status.lastSyncedAt());
    }

    @Test
    @DisplayName("A burst of changes to one event is written once")
    void changesAreCoalesced() {
        ScheduledExecutorService recording = mock(ScheduledExecutorService.class);
        AttendanceLiveSheet liveSheet = new AttendanceLiveSheet(
                enabled(), eventRepository, attendeeRepository, new EventExportService(),
                transaction(), recording, Duration.ofSeconds(4), Clock.fixed(NOW, ZoneOffset.UTC));

        for (int i = 0; i < 5; i++) {
            liveSheet.onAttendanceChanged(new AttendanceChangedEvent(7L));
        }

        verify(recording, times(1)).schedule(any(Runnable.class), eq(4000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    @DisplayName("Switched off, a change does nothing and the status says so")
    void disabledIsANoOp() {
        ScheduledExecutorService recording = mock(ScheduledExecutorService.class);
        AttendanceLiveSheet liveSheet = new AttendanceLiveSheet(
                AttendanceLiveSheet.Setup.off(), eventRepository, attendeeRepository,
                new EventExportService(), transaction(), recording, Duration.ofSeconds(4),
                Clock.fixed(NOW, ZoneOffset.UTC));

        liveSheet.onAttendanceChanged(new AttendanceChangedEvent(7L));

        verify(recording, never()).schedule(any(Runnable.class), anyLong(), any());
        assertFalse(liveSheet.status(7L).enabled());
    }

    @Test
    @DisplayName("A broken configuration is reported on the status, not thrown")
    void brokenSetupIsReported() {
        LiveSheetStatusDto status = liveSheet(AttendanceLiveSheet.Setup.broken("key is bad")).status(7L);

        assertFalse(status.enabled());
        assertEquals("key is bad", status.setupProblem());
    }

    @Test
    @DisplayName("Syncing an event that does not exist is a 404")
    void unknownEventIs404() {
        when(eventRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> liveSheet(enabled()).syncNow(99L));
    }

    // -- helpers ---------------------------------------------------------------

    private AttendanceLiveSheet liveSheet(AttendanceLiveSheet.Setup setup) {
        return new AttendanceLiveSheet(
                setup, eventRepository, attendeeRepository, new EventExportService(),
                transaction(), executor, Duration.ofMillis(10), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AttendanceLiveSheet.Setup enabled() {
        return new AttendanceLiveSheet.Setup(sheets, SPREADSHEET, ROBOT, null);
    }

    /** Runs the callback inline; the transaction manager is a mock. */
    private static TransactionTemplate transaction() {
        return new TransactionTemplate(mock(PlatformTransactionManager.class));
    }

    private EventAttendee attendee(Long userId, String name, String email) {
        User user = User.builder().id(userId).name(name).email(email).build();
        return EventAttendee.builder()
                .id(userId)
                .event(event)
                .user(user)
                .addedAt(LocalDateTime.of(2026, 9, 18, 10, userId.intValue()))
                .build();
    }

    /** An in-memory spreadsheet. */
    private static final class FakeSheets implements SheetsApi {
        final List<SheetTab> tabs = new ArrayList<>();
        final java.util.Map<Integer, List<List<String>>> contents = new java.util.HashMap<>();
        int addCalls;
        RuntimeException failWith;

        @Override
        public List<SheetTab> listTabs(String spreadsheetId) {
            if (failWith != null) {
                throw failWith;
            }
            return List.copyOf(tabs);
        }

        @Override
        public SheetTab addTab(String spreadsheetId, String title, int rowCount, int columnCount) {
            addCalls++;
            SheetTab tab = new SheetTab(100 + tabs.size(), title, rowCount, columnCount);
            tabs.add(tab);
            return tab;
        }

        @Override
        public void replaceTab(String spreadsheetId, SheetTab tab, String title, List<List<String>> rows) {
            tabs.replaceAll(t -> t.sheetId() == tab.sheetId()
                    ? new SheetTab(t.sheetId(), title, Math.max(t.rowCount(), rows.size()), t.columnCount())
                    : t);
            contents.put(tab.sheetId(), rows);
        }
    }
}
