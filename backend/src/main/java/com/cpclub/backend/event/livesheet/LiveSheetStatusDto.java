package com.cpclub.backend.event.livesheet;

import java.time.Instant;

/**
 * Where an event's live attendance sheet is, and whether the last update worked.
 *
 * <p>Shown on the admin event page. Everything an admin needs to fix a problem
 * without reading server logs: which address to share the spreadsheet with, and
 * Google's reason in plain words when an update failed.</p>
 *
 * @param enabled whether the server is set up to keep a live sheet at all
 * @param setupProblem why it is not, when the configuration is present but unusable
 * @param sheetUrl link to this event's tab, or to the spreadsheet before the first update
 * @param serviceAccountEmail the address the spreadsheet must be shared with, as Editor
 * @param lastSyncedAt when the last successful update finished, since the server started
 * @param lastError why the most recent update failed, or null if it succeeded
 */
public record LiveSheetStatusDto(
        boolean enabled,
        String setupProblem,
        String sheetUrl,
        String serviceAccountEmail,
        Instant lastSyncedAt,
        String lastError
) {
}
