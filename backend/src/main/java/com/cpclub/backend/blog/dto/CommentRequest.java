package com.cpclub.backend.blog.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param body     plain text (D19); trimmed, then 1 to 2000 characters
 * @param parentId a top-level comment on the same post, to reply to it; null otherwise
 */
public record CommentRequest(
        @NotBlank(message = "A comment cannot be empty")
        String body,
        Long parentId
) {
}
