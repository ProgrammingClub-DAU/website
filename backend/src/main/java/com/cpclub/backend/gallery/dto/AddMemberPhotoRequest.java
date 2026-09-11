package com.cpclub.backend.gallery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Admin payload for adding a batch-wise member gallery photo.
 *
 * <p>The browser uploads directly to Cloudinary; this payload only stores the
 * returned URL and its gallery metadata.</p>
 *
 * @param batchYear admission batch represented by the photo
 * @param imageUrl Cloudinary URL for the uploaded image
 * @param caption optional photo caption
 */
public record AddMemberPhotoRequest(
        @NotNull(message = "Batch year must not be null")
        @Min(value = 2000, message = "Batch year must be at least 2000")
        @Max(value = 2100, message = "Batch year must be at most 2100")
        Integer batchYear,

        @NotBlank(message = "Image URL must not be blank")
        @Size(max = 512, message = "Image URL must be at most 512 characters")
        String imageUrl,

        @Size(max = 255, message = "Caption must be at most 255 characters")
        String caption
) {
}
