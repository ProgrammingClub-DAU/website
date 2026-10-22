package com.cpclub.backend.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * New content for a post. Never changes its status: that goes through submit,
 * publish and reject, so there is exactly one way into each state.
 *
 * @param published accepted for compatibility with the old CMS and ignored
 */
public record BlogUpdateRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,
        @NotBlank(message = "Content is required")
        String content,
        @Size(max = 255, message = "Tags must be at most 255 characters in total")
        String tags,
        Boolean published
) {
}
