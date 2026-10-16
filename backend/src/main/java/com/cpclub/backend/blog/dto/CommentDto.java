package com.cpclub.backend.blog.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One comment, with its replies if it is top-level.
 *
 * @param body      null when deleted: a deleted comment with replies keeps its place
 *                  in the thread as "[deleted]" so the replies still make sense
 * @param canEdit   whether the caller may edit it now: their own, not deleted, and
 *                  inside the 15-minute window
 * @param canDelete whether the caller may delete it: their own, or they are an admin
 */
public record CommentDto(
        Long id,
        Long parentId,
        Author author,
        String body,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        boolean deleted,
        boolean canEdit,
        boolean canDelete,
        List<CommentDto> replies
) {
    /** Public identity only -- never email or phone. */
    public record Author(Long id, String name, String avatarUrl, String codeforcesHandle) {
    }
}
