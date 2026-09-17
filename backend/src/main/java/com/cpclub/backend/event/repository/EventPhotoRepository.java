package com.cpclub.backend.event.repository;

import com.cpclub.backend.event.entity.EventPhoto;
import com.cpclub.backend.event.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for {@link EventPhoto}.
 */
@Repository
public interface EventPhotoRepository extends JpaRepository<EventPhoto, Long> {

    /**
     * An event's photos in upload order, which is the order they are displayed.
     *
     * @param eventId event to list
     * @return photos, earliest upload first
     */
    List<EventPhoto> findByEventIdOrderByUploadedAtAsc(Long eventId);

    /**
     * Every photo from an event the public can see, with its event.
     *
     * <p>Cancelled events are excluded because they are excluded everywhere else
     * the public looks -- a gallery tile that links to an event the site will not
     * list is a dead end. The event is fetch-joined because every tile names it.</p>
     *
     * @param excluded the status to leave out; always {@code CANCELLED}
     */
    @Query("SELECT p FROM EventPhoto p JOIN FETCH p.event e WHERE e.status <> :excluded")
    List<EventPhoto> findAllWithEventExcludingStatus(@Param("excluded") EventStatus excluded);
}
