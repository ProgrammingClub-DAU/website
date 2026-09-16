package com.cpclub.backend.event.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Sets an event's podium, replacing whatever was there.
 *
 * <p>The whole podium goes in one call rather than a placing at a time. The
 * constraints that matter are between the placings -- no two firsts, nobody
 * standing twice -- and those can only be checked against a complete list. An
 * empty list clears the podium, which is how a mistake is undone.</p>
 *
 * @param winners at most three placings, in any order
 */
public record SetEventWinnersRequest(

        @NotNull(message = "Winners must not be null; send an empty list to clear the podium")
        @Size(max = 3, message = "An event has at most three placings")
        List<@Valid Winner> winners
) {

    /**
     * One placing.
     *
     * <p>Identifies the winner by member id, not by name. They must already be
     * recorded as having attended the event -- checked in the service, because
     * announcing a winner who was never marked present is almost always a
     * mis-click rather than an intention.</p>
     *
     * @param position 1, 2 or 3
     * @param userId the member who placed
     */
    public record Winner(

            @NotNull(message = "Position is required")
            @Min(value = 1, message = "Position must be 1, 2 or 3")
            @Max(value = 3, message = "Position must be 1, 2 or 3")
            Integer position,

            @NotNull(message = "Winner must be an existing member")
            Long userId
    ) {
    }
}
