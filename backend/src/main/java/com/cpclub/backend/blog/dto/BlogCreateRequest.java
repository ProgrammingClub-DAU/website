package com.cpclub.backend.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,

        @NotBlank(message = "Content is required")
        String content,

        String authorName,
        String tags,
        Boolean published
) {
}
