package com.cpclub.backend.event.entity;

/**
 * What kind of event this is.
 *
 * <p>Presentation, not behaviour: nothing in the application branches on this.
 * It is the badge on the public timeline, so a visitor scanning the club's
 * history can tell a flagship contest from a beginners' workshop without reading
 * every description.</p>
 *
 * <p>Separate from {@code EventStatus}, which says whether an event is upcoming,
 * finished or cancelled. An event has both: a WORKSHOP that is COMPLETED.</p>
 *
 * <p>Deliberately short. A list long enough to describe every event exactly is a
 * list nobody picks from consistently, and the badge stops being scannable --
 * which is the only thing it is for.</p>
 */
public enum EventType {

    /** The campus-wide contest the club builds its year around. */
    FLAGSHIP,

    /** An ordinary rated round. */
    CONTEST,

    /** A teaching session. */
    WORKSHOP,

    /** ICPC practice or preparation. */
    ICPC,

    /** A talk or guest session. */
    TALK
}
