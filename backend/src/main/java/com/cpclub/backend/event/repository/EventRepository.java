package com.cpclub.backend.event.repository;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Event}.
 *
 * <p>The ordering differs by status on purpose. Upcoming events read forwards â€”
 * the next one first â€” while completed events read backwards, most recent first.
 * A single ordering would put either the furthest-off event or the oldest one at
 * the top of its list.</p>
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Events in one status, soonest first. Backs the public upcoming list.
     *
     * @param status status to match
     * @return matching events, earliest date first
     */
    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    /**
     * Every event, most recent first. The admin view, which shows cancelled
     * events too.
     *
     * @return all events, latest date first
     */
    List<Event> findAllByOrderByEventDateDesc();

    /**
     * Events in any of several statuses, most recent first.
     *
     * @param statuses statuses to match
     * @return matching events, latest date first
     */
    List<Event> findByStatusInOrderByEventDateDesc(List<EventStatus> statuses);

    /**
     * Events in a date range and specific statuses.
     */
    List<Event> findByEventDateBetweenAndStatusIn(java.time.LocalDateTime from, java.time.LocalDateTime to, List<EventStatus> statuses);

    /**
     * Reads the event with a row lock ({@code SELECT ... FOR UPDATE}), held until the
     * surrounding transaction ends. An RSVP takes it before counting registrations,
     * so two requests for the last seat are serialised and cannot both see a free one.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
