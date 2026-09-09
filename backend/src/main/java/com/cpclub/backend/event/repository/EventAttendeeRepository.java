package com.cpclub.backend.event.repository;

import com.cpclub.backend.event.entity.EventAttendee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link EventAttendee}, the club's attendance record.
 */
@Repository
public interface EventAttendeeRepository extends JpaRepository<EventAttendee, Long> {

    /**
     * The attendance list for one event, in the order people were added.
     *
     * <p>Fetches the attendee's user in the same statement. Every field of
     * {@code EventAttendeeDto} comes off that association, so without the join
     * this is one query for the list and one per row to populate it — the export
     * of a well-attended event would issue hundreds.</p>
     *
     * @param eventId event to list
     * @return attendees, earliest addition first
     */
    @Query("""
            SELECT a FROM EventAttendee a
            JOIN FETCH a.user
            WHERE a.event.id = :eventId
            ORDER BY a.addedAt ASC
            """)
    List<EventAttendee> findByEventIdOrderByAddedAtAsc(Long eventId);

    /**
     * Whether a member is already on an event's list.
     *
     * <p>The pair is a unique constraint, so without this check a duplicate add
     * surfaces as a constraint violation at flush time rather than a 400 naming
     * the problem.</p>
     *
     * @param eventId event to check
     * @param userId member to check
     * @return whether the member is already registered
     */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * How many members attended, without loading them.
     *
     * <p>The public event page shows a headcount. Fetching the list to call
     * {@code size()} on it would pull every attendee's phone number and email out
     * of the database to render a single number.</p>
     *
     * @param eventId event to count
     * @return number of attendees recorded
     */
    long countByEventId(Long eventId);

    /**
     * Locates one attendance row, for removal.
     *
     * @param eventId event to check
     * @param userId member to check
     * @return the row when it exists
     */
    Optional<EventAttendee> findByEventIdAndUserId(Long eventId, Long userId);
}
