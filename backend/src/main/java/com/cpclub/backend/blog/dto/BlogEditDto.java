package com.cpclub.backend.blog.dto;

import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;

import java.time.LocalDateTime;

/**
 * A post as its author or an admin sees it: everything public, plus where it stands
 * in review. Only {@code getForEditing}, {@code mine} and the review queue return it.
 */
public record BlogEditDto(
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
        int readMinutes,
        String reviewNote,
        LocalDateTime submittedAt
) {
    public static BlogEditDto fromEntity(BlogPost post, long commentCount) {
        BlogResponseDto base = BlogResponseDto.fromEntity(post, commentCount);
        return new BlogEditDto(
                base.id(), base.title(), base.slug(), base.content(), base.authorName(), base.tags(),
                base.published(), base.createdAt(), base.updatedAt(), base.status(), base.authorId(),
                base.publishedAt(), base.commentCount(), base.readMinutes(),
                post.getReviewNote(), post.getSubmittedAt());
    }
}
