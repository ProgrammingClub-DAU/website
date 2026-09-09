package com.cpclub.backend.event.dto;

import com.cpclub.backend.event.entity.EventPhoto;

import java.time.LocalDateTime;

/**
 * A single photo attached to an event.
 *
 * <p>Omits the uploader. The gallery is a record of the event, not of which admin
 * happened to upload each frame.</p>
 *
 * @param id photo identifier
 * @param imageUrl Cloudinary URL
 * @param caption optional caption
 * @param uploadedAt upload timestamp
 */
public record EventPhotoDto(
        Long id,
        String imageUrl,
        String caption,
        LocalDateTime uploadedAt
) {
    /**
     * Maps a persisted photo into its API representation.
     *
     * @param photo persisted photo
     * @return public photo projection
     */
    public static EventPhotoDto fromEntity(EventPhoto photo) {
        return new EventPhotoDto(
                photo.getId(),
                photo.getImageUrl(),
                photo.getCaption(),
                photo.getUploadedAt()
        );
    }
}
