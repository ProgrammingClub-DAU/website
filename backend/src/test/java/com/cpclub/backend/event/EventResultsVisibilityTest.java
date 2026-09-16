package com.cpclub.backend.event;

import com.cpclub.backend.event.dto.EventDetailDto;
import com.cpclub.backend.event.dto.EventResponseDto;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.entity.EventWinner;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the public may see of an event's results.
 *
 * <p>Three things are withheld independently -- the contest link, the podium and
 * the turnout -- because the club announces them at different moments. A round
 * scheduled for Friday must not have its link readable on Wednesday, and that is
 * a property of the payload, not of what the page chooses to render: a link sent
 * and then hidden by a component is still in the page source.</p>
 *
 * <p>These assert absence, which is the awkward direction to test and the one
 * that matters. Each switch is checked both off and on, so a projection that
 * simply never sends the field would fail too.</p>
 */
class EventResultsVisibilityTest {

    private static final String CONTEST = "https://codeforces.com/contest/1234";

    // ------------------------------------------------------------ contest link

    @Test
    @DisplayName("An unpublished contest link does not reach the public at all")
    void contestLinkIsWithheldUntilPublished() {
        Event event = event();
        event.setShowContestLink(false);

        assertNull(EventResponseDto.fromEntity(event, false).codeforcesContestUrl());
        assertNull(detail(event, false).codeforcesContestUrl());
    }

    @Test
    @DisplayName("Publishing the contest link releases it to the public")
    void contestLinkIsReleasedOncePublished() {
        Event event = event();
        event.setShowContestLink(true);

        assertEquals(CONTEST, EventResponseDto.fromEntity(event, false).codeforcesContestUrl());
        assertEquals(CONTEST, detail(event, false).codeforcesContestUrl());
    }

    @Test
    @DisplayName("An admin sees the contest link before it is published, to check it")
    void adminsSeeTheContestLinkRegardless() {
        Event event = event();
        event.setShowContestLink(false);

        assertEquals(CONTEST, EventResponseDto.fromEntity(event, true).codeforcesContestUrl());
        assertEquals(CONTEST, detail(event, true).codeforcesContestUrl());
    }

    // ------------------------------------------------------------------ podium

    @Test
    @DisplayName("An unpublished podium is empty for the public, not merely unrendered")
    void winnersAreWithheldUntilPublished() {
        Event event = eventWithPodium();
        event.setShowWinners(false);

        assertTrue(detail(event, false).winners().isEmpty());
    }

    @Test
    @DisplayName("A published podium arrives in placing order")
    void winnersAreReleasedOncePublished() {
        Event event = eventWithPodium();
        event.setShowWinners(true);

        assertEquals(
                List.of(1, 2, 3),
                detail(event, false).winners().stream().map(w -> w.position()).toList());
    }

    @Test
    @DisplayName("An admin sees the podium before it is announced")
    void adminsSeeThePodiumRegardless() {
        Event event = eventWithPodium();
        event.setShowWinners(false);

        assertEquals(3, detail(event, true).winners().size());
    }

    @Test
    @DisplayName("A winner's contact details are never part of the podium")
    void thePodiumCarriesNoContactDetails() {
        // A placing is not a reason to publish what the member's own profile
        // would have withheld. EventWinnerDto has no email or phone field at all,
        // so this is checked by the record's shape; the name and handle it does
        // carry are already public on the members page.
        Event event = eventWithPodium();
        event.setShowWinners(true);

        var winner = detail(event, false).winners().get(0);
        assertEquals("Winner 1", winner.name());
        assertEquals("winner1", winner.codeforcesHandle());
    }

    // ----------------------------------------------------------------- turnout

    @Test
    @DisplayName("An unpublished turnout is absent rather than zero")
    void attendeeCountIsWithheldUntilPublished() {
        Event event = event();
        event.setShowAttendeeCount(false);

        // Null, not 0. A zero would read as "nobody came", which is a different
        // and much worse claim than "we have not said".
        assertNull(detail(event, false).attendeeCount());
    }

    @Test
    @DisplayName("A published turnout reaches the public")
    void attendeeCountIsReleasedOncePublished() {
        Event event = event();
        event.setShowAttendeeCount(true);

        assertEquals(42, detail(event, false).attendeeCount());
    }

    @Test
    @DisplayName("An admin always sees the real turnout")
    void adminsSeeTheTurnoutRegardless() {
        Event event = event();
        event.setShowAttendeeCount(false);

        assertEquals(42, detail(event, true).attendeeCount());
    }

    // ------------------------------------------------------- never gated

    @Test
    @DisplayName("Title, date, place and description are public whatever the switches say")
    void theEventItselfIsAlwaysPublic() {
        // This is what an event is before it has results, and what a workshop
        // stays. Gating it would leave the public page blank.
        Event event = event();

        EventDetailDto dto = detail(event, false);

        assertEquals("Spring Sprint", dto.title());
        assertEquals("Lab 101", dto.location());
        assertEquals("Five problems, two hours", dto.description());
        assertEquals(LocalDateTime.of(2026, 10, 3, 15, 0), dto.eventDate());
    }

    // ------------------------------------------------------------------ helpers

    private EventDetailDto detail(Event event, boolean viewerIsAdmin) {
        return EventDetailDto.of(event, List.of(), 42, viewerIsAdmin);
    }

    private Event event() {
        return Event.builder()
                .id(10L)
                .title("Spring Sprint")
                .description("Five problems, two hours")
                .eventDate(LocalDateTime.of(2026, 10, 3, 15, 0))
                .location("Lab 101")
                .status(EventStatus.COMPLETED)
                .codeforcesContestUrl(CONTEST)
                .build();
    }

    private Event eventWithPodium() {
        Event event = event();
        for (int position = 1; position <= 3; position++) {
            User member = new User("Winner " + position, "w" + position + "@dau.ac.in", null, Role.ROLE_USER);
            member.setId((long) position);
            member.setCodeforcesHandle("winner" + position);
            member.setPhoneNumber("9876543210");

            event.getWinners().add(EventWinner.builder()
                    .event(event)
                    .user(member)
                    .position(position)
                    .build());
        }
        return event;
    }
}
