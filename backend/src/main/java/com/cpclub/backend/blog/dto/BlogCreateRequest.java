package com.cpclub.backend.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A new post. Any signed-in member may write one; it starts as a DRAFT (D18).
 *
 * @param authorName admin only: credit a post written for someone else. Ignored for
 *                   members, whose posts always carry their own name.
 * @param published  legacy flag from the admin-only CMS. Ignored unless the caller is
 *                   an admin, and then it means the same as {@code publishNow}.
 * @param publishNow admin only: skip review and publish immediately.
 */
public record BlogCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
        String title,
        @NotBlank(message = "Content is required")
        String content,
        String authorName,
        String tags,
        Boolean published,
        Boolean publishNow
) {
}
