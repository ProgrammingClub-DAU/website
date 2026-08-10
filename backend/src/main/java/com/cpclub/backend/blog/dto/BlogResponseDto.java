package com.cpclub.backend.blog.dto;

import com.cpclub.backend.blog.entity.BlogPost;

import java.time.LocalDateTime;

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
