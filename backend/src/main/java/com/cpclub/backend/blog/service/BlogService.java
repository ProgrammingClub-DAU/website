package com.cpclub.backend.blog.service;

import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.dto.BlogUpdateRequest;
import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.repository.BlogRepository;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class managing blog post lifecycle operations including
 * creation with auto-generated slugs, updates, deletion, and paginated reads.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlogService {

    private final BlogRepository blogRepository;

    /**
     * Retrieves published blog posts sorted by creation date descending.
     *
     * @param page zero-indexed page number
     * @param size items per page
     * @return paginated wrapper of published blog post DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<BlogResponseDto> getPublishedBlogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BlogPost> blogPage = blogRepository.findByPublishedTrue(pageable);

        List<BlogResponseDto> content = blogPage.getContent().stream()
                .map(BlogResponseDto::fromEntity)
                .toList();

        return new PagedResponse<>(
                content,
                blogPage.getNumber(),
                blogPage.getSize(),
                blogPage.getTotalElements(),
                blogPage.getTotalPages(),
                blogPage.isLast()
        );
    }

    /**
     * Fetches a blog post by its database primary key.
     *
     * @param id blog post ID
     * @return immutable blog post DTO
     * @throws ResourceNotFoundException if ID does not exist
     */
    @Transactional(readOnly = true)
    public BlogResponseDto getBlogById(Long id) {
        BlogPost post = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));
        return BlogResponseDto.fromEntity(post);
    }

    /**
     * Fetches a blog post by its URL-friendly slug identifier.
     *
     * @param slug SEO-friendly URL slug
     * @return immutable blog post DTO
     * @throws ResourceNotFoundException if slug does not exist
     */
    @Transactional(readOnly = true)
    public BlogResponseDto getBlogBySlug(String slug) {
        BlogPost post = blogRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with slug: " + slug));
        return BlogResponseDto.fromEntity(post);
    }

    /**
     * Creates a new blog post with an auto-generated slug from the title.
     * If a slug collision occurs, appends a timestamp suffix to ensure uniqueness.
     *
     * @param request creation payload with title, content, tags
     * @param defaultAuthor fallback author name from the authenticated principal
     * @return the persisted blog post DTO
     */
    @Transactional
    public BlogResponseDto createBlog(BlogCreateRequest request, String defaultAuthor) {
        String slug = generateSlug(request.title());
        if (blogRepository.existsBySlug(slug)) {
            // Append timestamp to resolve slug collision
            slug = slug + "-" + System.currentTimeMillis();
        }

        String author = (request.authorName() != null && !request.authorName().isBlank())
                ? request.authorName()
                : defaultAuthor;

        BlogPost post = BlogPost.builder()
                .title(request.title().trim())
                .slug(slug)
                .content(request.content())
                .authorName(author)
                .tags(request.tags())
                .published(request.published() == null || request.published())
                .build();

        BlogPost saved = blogRepository.save(post);
        log.info("Created blog post '{}' with ID {}", saved.getTitle(), saved.getId());
        return BlogResponseDto.fromEntity(saved);
    }

    /**
     * Updates an existing blog post's title, content, tags, and publish status.
     *
     * @param id blog post ID
     * @param request update payload
     * @return the updated blog post DTO
     * @throws ResourceNotFoundException if ID does not exist
     */
    @Transactional
    public BlogResponseDto updateBlog(Long id, BlogUpdateRequest request) {
        BlogPost post = blogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + id));

        post.setTitle(request.title().trim());
        post.setContent(request.content());
        if (request.tags() != null) {
            post.setTags(request.tags());
        }
        if (request.published() != null) {
            post.setPublished(request.published());
        }

        BlogPost saved = blogRepository.save(post);
        log.info("Updated blog post ID {}", saved.getId());
        return BlogResponseDto.fromEntity(saved);
    }

    /**
     * Permanently deletes a blog post by ID.
     *
     * @param id blog post ID
     * @throws ResourceNotFoundException if ID does not exist
     */
    @Transactional
    public void deleteBlog(Long id) {
        if (!blogRepository.existsById(id)) {
            throw new ResourceNotFoundException("Blog post not found with id: " + id);
        }
        blogRepository.deleteById(id);
        log.info("Deleted blog post ID {}", id);
    }

    /**
     * Generates a URL-friendly slug from a blog title by lowercasing,
     * stripping special characters, and replacing whitespace with hyphens.
     *
     * @param title raw blog post title
     * @return sanitized slug string
     */
    private String generateSlug(String title) {
        return title.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}

