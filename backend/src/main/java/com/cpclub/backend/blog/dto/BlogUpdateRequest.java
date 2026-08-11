package com.cpclub.backend.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Immutable request DTO for updating an existing blog post.
 * Tags and published status are optional — only non-null values are applied.
 */
public record BlogUpdateRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        String tags,
        Boolean published
) {
}
