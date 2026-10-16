package com.cpclub.backend.blog.dto;

import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;

import java.time.LocalDateTime;

/**
 * A post as anyone may read it.
 *
 * @param commentCount visible comments: deleted ones are not counted
 * @param readMinutes  {@code max(1, round(words / 200))}, computed here so every
 *                     client shows the same figure
 */
public record BlogResponseDto(
        Long id,
        String title,
        String slug,
        String content,
        String authorName,
        String tags,
        Boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        BlogStatus status,
        Long authorId,
        LocalDateTime publishedAt,
        long commentCount,
        int readMinutes
) {
    public static BlogResponseDto fromEntity(BlogPost post, long commentCount) {
        return new BlogResponseDto(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getAuthorName(),
                post.getTags(),
                post.getStatus() == BlogStatus.PUBLISHED,
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getStatus(),
                post.getAuthor() != null ? post.getAuthor().getId() : null,
                post.getPublishedAt(),
                commentCount,
                readMinutes(post.getContent())
        );
    }

    static int readMinutes(String content) {
        if (content == null || content.isBlank()) {
            return 1;
        }
        int words = content.trim().split("\\s+").length;
        return Math.max(1, (int) Math.round(words / 200.0));
    }
}
