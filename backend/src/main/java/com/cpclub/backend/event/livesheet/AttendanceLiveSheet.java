package com.cpclub.backend.event.livesheet;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.event.dto.EventAttendeeDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Keeps one Google Sheets tab per event in step with its attendance list.
 *
 * <p><b>What it writes.</b> One spreadsheet for the club, configured on the
 * server, with a tab per event named {@code "#<event id> <event title>"}. The
 * tab holds the same six columns as the .xlsx download, built by
 * {@link EventExportService}, so the two can never disagree. The id prefix is
 * how a tab is found again after the event is renamed.</p>
 *
 * <p><b>How.</b> Every update rewrites the whole tab from the database rather
 * than appending the one row that changed. A removal, a corrected name or a
 * missed update are then all fixed by the next write, and the sheet cannot drift
 * away from the attendance list over time.</p>
 *
 * <p><b>When.</b> A few seconds after a change is committed. Changes arriving
 * within that window share one write, so marking thirty people present in a
 * minute costs a handful of calls to Google, not thirty. All writes run on one
 * background thread, one at a time, so two updates never race to create the
 * same tab -- and an admin marking attendance never waits on Google.</p>
 *
 * <p><b>When it fails.</b> Attendance is saved regardless; the sheet is a copy.
 * The failure is logged and kept for the admin page, in words that say what to
 * fix, and the next change or a manual sync tries again.</p>
 */
@Slf4j
public class AttendanceLiveSheet {

    /** Sheets allows 100 characters in a tab name. */
    private static final int MAX_TAB_TITLE = 100;

    /** Characters Sheets rejects, or that break a reference to the tab, in a tab name. */
    private static final String UNSAFE_TAB_CHARACTERS = "[\\[\\]*?/\\\\:]";

    /** How long a manual sync may take before the admin is told to check back. */
    private static final Duration MANUAL_SYNC_TIMEOUT = Duration.ofSeconds(45);

    /**
     * The spreadsheet and the account that writes to it, or the reason there is
     * none.
     *
     * @param api the Sheets client, or null when disabled
     * @param spreadsheetId the club's attendance spreadsheet
     * @param serviceAccountEmail the account the spreadsheet is shared with
     * @param problem why the configuration is unusable, or null when it is fine
     */
    public record Setup(SheetsApi api, String spreadsheetId, String serviceAccountEmail, String problem) {

        /** Not configured at all: the feature is simply off. */
        public static Setup off() {
            return new Setup(null, null, null, null);
        }

        /** Configured, but not usably. Surfaced on the admin page, not thrown at startup. */
        public static Setup broken(String problem) {
            return new Setup(null, null, null, problem);
        }

        boolean enabled() {
            return api != null && spreadsheetId != null && !spreadsheetId.isBlank();
        }
    }

    /** What a tab should contain, read in one transaction. */
    private record Snapshot(String eventTitle, List<List<String>> rows) {
    }

    private final Setup setup;
    private final EventRepository eventRepository;
    private final EventAttendeeRepository attendeeRepository;
    private final EventExportService exportService;
    private final TransactionTemplate readOnlyTransaction;
    private final ScheduledExecutorService executor;
    private final Duration debounce;
    private final Clock clock;

    /** Events with an update already scheduled; a further change joins it. */
    private final Set<Long> pending = ConcurrentHashMap.newKeySet();

    /*
     * Status for the admin page, per event. In memory on purpose: it is a status
     * line, not a record, and after a restart the next update refills it.
     */

    /** Why the latest update failed. Absent once an update succeeds. */
    private final Map<Long, String> lastErrors = new ConcurrentHashMap<>();

    /** When the latest successful update finished. A failure does not move it. */
    private final Map<Long, Instant> lastSuccesses = new ConcurrentHashMap<>();

    /** Last known tab per event, so the admin page can link straight to it. */
    private final Map<Long, Integer> knownSheetIds = new ConcurrentHashMap<>();

    public AttendanceLiveSheet(
            Setup setup,
            EventRepository eventRepository,
            EventAttendeeRepository attendeeRepository,
            EventExportService exportService,
            TransactionTemplate readOnlyTransaction,
            ScheduledExecutorService executor,
            Duration debounce,
            Clock clock
    ) {
        this.setup = setup;
        this.eventRepository = eventRepository;
        this.attendeeRepository = attendeeRepository;
        this.exportService = exportService;
        this.readOnlyTransaction = readOnlyTransaction;
        this.executor = executor;
        this.debounce = debounce;
        this.clock = clock;
    }

    /** Stops the background thread when the application shuts down. */
    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * Schedules an update once the change that caused it has committed.
     *
     * <p>{@code fallbackExecution}: a change made outside a transaction still
     * updates the sheet, rather than being dropped silently.</p>
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onAttendanceChanged(AttendanceChangedEvent change) {
        requestSync(change.eventId());
    }

    /**
     * Updates an event's tab shortly, joining any update already waiting.
     *
     * <p>The event leaves {@code pending} before its update reads the database,
     * so a change landing during the write schedules another write rather than
     * being lost.</p>
     *
     * @param eventId the event whose tab is out of date
     */
    public void requestSync(Long eventId) {
        if (!setup.enabled() || eventId == null) {
            return;
        }
        if (pending.add(eventId)) {
            executor.schedule(() -> {
                pending.remove(eventId);
                sync(eventId);
            }, debounce.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Updates an event's tab now and reports the outcome.
     *
     * <p>For the admin's "Sync now" button, and for filling in the tab of an
     * event whose attendance was recorded before the live sheet was set up. Runs
     * on the same single thread as the automatic updates, and waits for it.</p>
     *
     * @param eventId the event
     * @return the status after the update
     * @throws ResourceNotFoundException if the event does not exist
     */
    public LiveSheetStatusDto syncNow(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Event not found with id: " + eventId);
        }
        if (setup.enabled()) {
            try {
                executor.submit(() -> sync(eventId))
                        .get(MANUAL_SYNC_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                // Still running on the background thread; the status will show
                // the result when it lands.
                log.warn("Live sheet sync for event {} is taking longer than {}", eventId, MANUAL_SYNC_TIMEOUT);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                // sync() records its own failures; anything reaching here is a bug.
                log.error("Live sheet sync for event {} failed unexpectedly", eventId, e.getCause());
            }
        }
        return status(eventId);
    }

    /**
     * Where an event's tab is and how its last update went.
     *
     * @param eventId the event
     * @return the status for the admin page
     */
    public LiveSheetStatusDto status(Long eventId) {
        if (!setup.enabled()) {
            return new LiveSheetStatusDto(false, setup.problem(), null, null, null, null);
        }

        Integer sheetId = knownSheetIds.get(eventId);
        String url = "https://docs.google.com/spreadsheets/d/" + setup.spreadsheetId() + "/edit"
                + (sheetId != null ? "#gid=" + sheetId : "");

        return new LiveSheetStatusDto(
                true,
                null,
                url,
                setup.serviceAccountEmail(),
                lastSuccesses.get(eventId),
                lastErrors.get(eventId)
        );
    }

    /**
     * One full rewrite of an event's tab. Never throws: every failure is
     * recorded for the admin page instead.
     */
    private void sync(Long eventId) {
        try {
            Optional<Snapshot> snapshot = readSnapshot(eventId);
            if (snapshot.isEmpty()) {
                // Deleted between the change and the update. Its tab stays as the
                // last record of the event; nothing to write.
                return;
            }

            String title = tabTitle(eventId, snapshot.get().eventTitle());
            List<List<String>> rows = snapshot.get().rows();
            List<SheetsApi.SheetTab> tabs = setup.api().listTabs(setup.spreadsheetId());

            SheetsApi.SheetTab tab = findTab(tabs, eventId).orElseGet(() -> setup.api().addTab(
                    setup.spreadsheetId(),
                    title,
                    Math.max(rows.size(), 100),
                    rows.isEmpty() ? 1 : rows.get(0).size()
            ));

            setup.api().replaceTab(setup.spreadsheetId(), tab, title, rows);

            knownSheetIds.put(eventId, tab.sheetId());
            lastSuccesses.put(eventId, clock.instant());
            lastErrors.remove(eventId);
            log.info("Live sheet: wrote {} attendees for event {}", rows.size() - 1, eventId);

        } catch (RuntimeException e) {
            String reason = describe(e);
            lastErrors.put(eventId, reason);
            log.warn("Live sheet: update for event {} failed: {}", eventId, reason);
        }
    }

    private Optional<Snapshot> readSnapshot(Long eventId) {
        return Optional.ofNullable(readOnlyTransaction.execute(status -> {
            Event event = eventRepository.findById(eventId).orElse(null);
            if (event == null) {
                return null;
            }
            List<EventAttendeeDto> attendees = attendeeRepository.findByEventIdOrderByAddedAtAsc(eventId)
                    .stream()
                    .map(EventAttendeeDto::fromEntity)
                    .toList();
            return new Snapshot(event.getTitle(), exportService.toSheetRows(attendees));
        }));
    }

    /**
     * This event's tab: the one whose name starts with its id.
     *
     * <p>The space after the id matters. Without it, event 12 would claim the
     * tab of event 123.</p>
     */
    static Optional<SheetsApi.SheetTab> findTab(List<SheetsApi.SheetTab> tabs, Long eventId) {
        String prefix = "#" + eventId;
        return tabs.stream()
                .filter(tab -> tab.title().equals(prefix) || tab.title().startsWith(prefix + " "))
                .findFirst();
    }

    /**
     * "#12 IPC 2026", made safe for a tab name.
     *
     * @param eventId the event's id, which keeps the name unique and findable
     * @param eventTitle the event's title
     * @return a valid tab name of at most 100 characters
     */
    static String tabTitle(Long eventId, String eventTitle) {
        String cleaned = eventTitle == null
                ? ""
                : eventTitle.replaceAll(UNSAFE_TAB_CHARACTERS, " ").replaceAll("\\s+", " ").trim();
        String title = cleaned.isEmpty() ? "#" + eventId : "#" + eventId + " " + cleaned;
        return title.length() <= MAX_TAB_TITLE ? title : title.substring(0, MAX_TAB_TITLE).trim();
    }

    /**
     * Google's refusal, in words an admin can act on.
     *
     * <p>The raw error names an HTTP status and a JSON body; the admin needs to
     * know whether to share the spreadsheet, fix a variable, or wait.</p>
     */
    String describe(RuntimeException e) {
        if (e instanceof HttpStatusCodeException http) {
            int code = http.getStatusCode().value();
            String body = http.getResponseBodyAsString();
            if (body.contains("invalid_grant") || body.contains("invalid_client")) {
                return "Google rejected the service-account key. Create a new JSON key and update "
                        + "GOOGLE_SERVICE_ACCOUNT_JSON on the server.";
            }
            if (code == 403 && body.contains("SERVICE_DISABLED")) {
                return "The Google Sheets API is turned off for this Cloud project. Enable it in "
                        + "APIs & Services > Library.";
            }
            if (code == 403) {
                return "Google refused access to the spreadsheet. Share it with "
                        + setup.serviceAccountEmail() + " as an Editor.";
            }
            if (code == 404) {
                return "The spreadsheet was not found. Check ATTENDANCE_SPREADSHEET_ID on the server.";
            }
            if (code == 429) {
                return "Google is rate-limiting updates. The next change will try again.";
            }
            return "Google returned an error (" + code + "). The next change will try again.";
        }
        if (e instanceof ResourceAccessException) {
            return "Could not reach Google. The next change will try again.";
        }
        return "The update failed: " + e.getMessage();
    }
}
