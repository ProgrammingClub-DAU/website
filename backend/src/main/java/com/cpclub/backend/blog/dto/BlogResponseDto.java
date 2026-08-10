package com.cpclub.backend.blog.dto;

import com.cpclub.backend.blog.entity.BlogPost;

import java.time.LocalDateTime;

/**
 * Immutable response DTO representing a blog post returned to API consumers.
 * Maps all public-facing fields from the {@link BlogPost} entity.
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
        LocalDateTime updatedAt
) {
    /**
     * Factory method mapping a JPA entity to an immutable response record.
     *
     * @param post persisted blog post entity
     * @return mapped DTO
     */
    public static BlogResponseDto fromEntity(BlogPost post) {
        return new BlogResponseDto(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                post.getContent(),
                post.getAuthorName(),
                post.getTags(),
                post.getPublished(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
