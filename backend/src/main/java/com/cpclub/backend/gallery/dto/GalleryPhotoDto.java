package com.cpclub.backend.gallery.dto;

import java.time.LocalDate;

/**
 * One photo in the public gallery, and where it came from.
 *
 * <p>The gallery is a single wall of photos drawn from two places -- events and
 * the Hall of Fame -- so each photo carries enough to say what it shows and to
 * link back: the kind of source, its id, its title, and its date.</p>
 *
 * @param id unique across the gallery: the source is part of it, because an event
 *           photo and a Hall of Fame photo can share a numeric id
 * @param location the event's venue; null for Hall of Fame photos
 */
public record GalleryPhotoDto(
        String id,
        String imageUrl,
        String caption,
        Source source,
        Long sourceId,
        String sourceTitle,
        LocalDate date,
        String location
) {

    /** Where a gallery photo belongs. */
    public enum Source {
        EVENT,
        HALL_OF_FAME
    }
}
