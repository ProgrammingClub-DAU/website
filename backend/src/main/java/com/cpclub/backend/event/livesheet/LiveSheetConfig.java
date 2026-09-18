package com.cpclub.backend.event.livesheet;

import com.cpclub.backend.event.repository.EventAttendeeRepository;
import com.cpclub.backend.event.repository.EventRepository;
import com.cpclub.backend.event.service.EventExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds the live attendance sheet from two environment variables.
 *
 * <ul>
 *   <li>{@code ATTENDANCE_SPREADSHEET_ID} -- the club's spreadsheet. Its id, or
 *       simply its URL.</li>
 *   <li>{@code GOOGLE_SERVICE_ACCOUNT_JSON} -- the service account's JSON key.</li>
 * </ul>
 *
 * <p>Neither set: the feature is off and nothing else changes. Set but unusable
 * -- one missing, or a key that does not parse -- the server still starts, and
 * the admin event page says what is wrong. A side feature with a typo in its
 * configuration must not take the whole site down.</p>
 */
@Configuration
@Slf4j
public class LiveSheetConfig {

    /** How long to wait after a change for more changes to join the same write. */
    static final Duration DEBOUNCE = Duration.ofSeconds(4);

    /** The id inside a spreadsheet URL: docs.google.com/spreadsheets/d/{id}/edit */
    private static final Pattern ID_IN_URL = Pattern.compile("/spreadsheets/d/([A-Za-z0-9_-]+)");

    /**
     * The live sheet, running on its own single background thread.
     *
     * <p>The executor is created here and not exposed as a bean on purpose.
     * Spring's scheduler adopts a lone {@code ScheduledExecutorService} bean for
     * every {@code @Scheduled} job, and this one thread would then also be running
     * the Codeforces and LeetCode syncs.</p>
     */
    @Bean(destroyMethod = "shutdown")
    public AttendanceLiveSheet attendanceLiveSheet(
            @Value("${cpclub.live-sheet.spreadsheet-id:}") String spreadsheetId,
            @Value("${cpclub.live-sheet.service-account-json:}") String serviceAccountJson,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            EventRepository eventRepository,
            EventAttendeeRepository attendeeRepository,
            EventExportService exportService,
            PlatformTransactionManager transactionManager
    ) {
        AttendanceLiveSheet.Setup setup = setup(spreadsheetId, serviceAccountJson, restTemplate, objectMapper);

        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "live-attendance-sheet");
            thread.setDaemon(true);
            return thread;
        });

        return new AttendanceLiveSheet(
                setup, eventRepository, attendeeRepository, exportService,
                readOnly, executor, DEBOUNCE, Clock.systemUTC());
    }

    static AttendanceLiveSheet.Setup setup(
            String spreadsheetId,
            String serviceAccountJson,
            RestTemplate restTemplate,
            ObjectMapper objectMapper
    ) {
        boolean hasId = spreadsheetId != null && !spreadsheetId.isBlank();
        boolean hasKey = serviceAccountJson != null && !serviceAccountJson.isBlank();

        if (!hasId && !hasKey) {
            log.info("Live attendance sheet is off: ATTENDANCE_SPREADSHEET_ID and "
                    + "GOOGLE_SERVICE_ACCOUNT_JSON are not set.");
            return AttendanceLiveSheet.Setup.off();
        }
        if (!hasId) {
            return broken("ATTENDANCE_SPREADSHEET_ID is not set on the server.");
        }
        if (!hasKey) {
            return broken("GOOGLE_SERVICE_ACCOUNT_JSON is not set on the server.");
        }

        ServiceAccountKey key;
        try {
            key = ServiceAccountKey.parse(serviceAccountJson, objectMapper);
        } catch (IllegalArgumentException e) {
            return broken(e.getMessage() + " Check GOOGLE_SERVICE_ACCOUNT_JSON on the server.");
        }

        GoogleAccessTokens tokens = new GoogleAccessTokens(key, restTemplate, Clock.systemUTC());
        log.info("Live attendance sheet is on, writing as {}", key.clientEmail());
        return new AttendanceLiveSheet.Setup(
                new RestSheetsApi(restTemplate, tokens),
                spreadsheetIdFrom(spreadsheetId),
                key.clientEmail(),
                null
        );
    }

    /**
     * The spreadsheet id, whether the variable holds the id or the whole URL.
     *
     * <p>Pasting the URL from the address bar is the natural thing to do, so it
     * is accepted rather than documented as a mistake.</p>
     */
    static String spreadsheetIdFrom(String value) {
        String trimmed = value.trim();
        Matcher matcher = ID_IN_URL.matcher(trimmed);
        return matcher.find() ? matcher.group(1) : trimmed;
    }

    private static AttendanceLiveSheet.Setup broken(String problem) {
        log.error("Live attendance sheet is misconfigured: {}", problem);
        return AttendanceLiveSheet.Setup.broken(problem);
    }
}
