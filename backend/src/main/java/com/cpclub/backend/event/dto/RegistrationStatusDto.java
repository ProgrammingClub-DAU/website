package com.cpclub.backend.event.dto;

/**
 * Where the caller stands on one event's RSVPs.
 *
 * @param registered whether the caller has RSVP'd
 * @param count      RSVPs so far
 * @param capacity   the cap, or null for unlimited
 * @param open       whether RSVPs are still accepted at all: the event is UPCOMING and
 *                   has not started. A full event is still open -- {@code count} and
 *                   {@code capacity} say whether a seat is free.
 */
public record RegistrationStatusDto(boolean registered, long count, Integer capacity, boolean open) {
}
