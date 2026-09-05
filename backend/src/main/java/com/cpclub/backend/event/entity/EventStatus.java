package com.cpclub.backend.event.entity;

/**
 * Lifecycle state of a club event.
 *
 * <p>Replaces an {@code is_active} boolean so the three real states are
 * distinguishable. Events are never hard-deleted -- cancelling one keeps its
 * attendance and photo history intact.</p>
 *
 * <p>Persisted as a string. The names must stay in step with the
 * {@code events_status_check} constraint in {@code V3__create_events.sql}.</p>
 */
public enum EventStatus {
    /** Publicly listed and accepting attendance. */
    UPCOMING,

    /** Publicly listed, read-only, photo gallery shown. */
    COMPLETED,

    /** Hidden from public listings; history retained. */
    CANCELLED
}
