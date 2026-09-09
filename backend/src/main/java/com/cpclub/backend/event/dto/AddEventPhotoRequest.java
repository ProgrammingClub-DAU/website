package com.cpclub.backend.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Admin payload for attaching a photo to an event.
 *
 * <p>Takes a URL rather than the file itself. Uploads go from the browser straight
 * to Cloudinary, so the image never passes through this server.</p>
 *
 * @param imageUrl Cloudinary URL of the uploaded image
 * @param caption optional caption
 */
public record AddEventPhotoRequest(
        @NotBlank(message = "Image URL must not be blank")
        @Size(max = 512, message = "Image URL must be at most 512 characters")
        String imageUrl,

        String caption
) {
}
