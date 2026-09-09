package com.cpclub.backend.event.repository;

import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for {@link Event}.
 *
 * <p>The ordering differs by status on purpose. Upcoming events read forwards —
 * the next one first — while completed events read backwards, most recent first.
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
}
