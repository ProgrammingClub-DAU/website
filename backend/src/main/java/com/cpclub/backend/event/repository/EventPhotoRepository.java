package com.cpclub.backend.event.repository;

import com.cpclub.backend.event.entity.EventPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
