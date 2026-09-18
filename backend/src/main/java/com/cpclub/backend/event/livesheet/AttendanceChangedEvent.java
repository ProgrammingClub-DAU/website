package com.cpclub.backend.event.livesheet;

/**
 * An event's attendance list, or something shown on its sheet, has changed.
 *
 * <p>Published by {@code EventService} inside the transaction that made the
 * change, and handled only after that transaction commits, so the sheet is never
 * written from data that was then rolled back.</p>
 *
 * @param eventId the event whose sheet is out of date
 */
public record AttendanceChangedEvent(Long eventId) {
}
