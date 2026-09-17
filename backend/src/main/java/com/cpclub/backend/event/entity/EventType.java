package com.cpclub.backend.event.entity;

/**
 * What kind of event this is.
 *
 * <p>Presentation, not behaviour: nothing in the application branches on this.
 * It is the badge on the public timeline, so a visitor scanning the club's
 * history can tell an IPC round from a lecture without reading
 * every description.</p>
 *
 * <p>Separate from {@code EventStatus}, which says whether an event is upcoming,
 * finished or cancelled. An event has both: a LECTURE that is COMPLETED.</p>
 *
 * <p>The club's own names for what it runs, from its orientation deck (V13).
 * Stored by name, so renaming a constant needs a migration.</p>
 */
public enum EventType {

    /** Intra College Programming Contest. */
    IPC,

    /** A contest for juniors. */
    JUNIORS_CONTEST,

    /** The inter-wing competition. */
    INTER_WING,

    /** Round Robin Relay. */
    ROUND_ROBIN_RELAY,

    /** A lecture session on competitive programming. */
    LECTURE,

    /** A post-contest discussion or editorial session. */
    POST_CONTEST_DISCUSSION
}
