package com.cpclub.backend.gallery.dto;

import com.cpclub.backend.gallery.entity.MemberGalleryPhoto;

import java.time.LocalDateTime;

/**
 * Public representation of a member-gallery photo.
 *
 * <p>The uploader is intentionally omitted: public gallery viewers need the
 * image metadata, not internal administrator relationships.</p>
 *
 * @param id photo identifier
 * @param batchYear admission batch represented by the photo
 * @param imageUrl Cloudinary URL
 * @param caption optional photo caption
 * @param uploadedAt upload timestamp
 */
public record MemberGalleryPhotoDto(
        Long id,
        Integer batchYear,
        String imageUrl,
        String caption,
        LocalDateTime uploadedAt
) {
    /**
     * Maps a persisted gallery photo into its public API representation.
     *
     * @param photo persisted gallery photo
     * @return public photo projection
     */
    public static MemberGalleryPhotoDto fromEntity(MemberGalleryPhoto photo) {
        return new MemberGalleryPhotoDto(
                photo.getId(),
                photo.getBatchYear(),
                photo.getImageUrl(),
                photo.getCaption(),
                photo.getUploadedAt()
        );
    }
}
