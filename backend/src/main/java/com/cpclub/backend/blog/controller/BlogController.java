package com.cpclub.backend.blog.controller;

import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.dto.BlogUpdateRequest;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the club's content management system.
 * Provides public read access to published articles and admin-only write operations.
 * Slugs are auto-generated from titles to support SEO-friendly URLs.
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs & Editorial CMS", description = "Endpoints for reading and publishing club articles, tutorials, and editorials")
public class BlogController {

    private final BlogService blogService;

    /**
     * Retrieves paginated list of published blog posts, ordered by recency.
     *
     * @param page zero-indexed page number
     * @param size number of items per page
     * @return paginated blog post summaries
     */
    @GetMapping
    @Operation(summary = "Get published blog posts (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<BlogResponseDto>>> getPublishedBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PagedResponse<BlogResponseDto> response = blogService.getPublishedBlogs(page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched blog posts successfully"));
    }

    /**
     * Fetches a single blog post by its database ID.
     *
     * @param id blog post primary key
     * @return full blog post details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get blog post by ID")
    public ResponseEntity<ApiResponse<BlogResponseDto>> getBlogById(@PathVariable Long id) {
        BlogResponseDto blog = blogService.getBlogById(id);
        return ResponseEntity.ok(ApiResponse.success(blog, "Fetched blog post successfully"));
    }

    /**
     * Fetches a single blog post by its URL-friendly slug.
     * Used by the frontend for SEO-friendly routing.
     *
     * @param slug URL-friendly identifier derived from the post title
     * @return full blog post details
     */
    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get blog post by slug")
    public ResponseEntity<ApiResponse<BlogResponseDto>> getBlogBySlug(@PathVariable String slug) {
        BlogResponseDto blog = blogService.getBlogBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(blog, "Fetched blog post successfully"));
    }

    /**
     * Creates a new blog post. Restricted to administrators.
     * Author defaults to the authenticated user's email if not explicitly provided.
     *
     * @param request validated blog creation payload
     * @param userDetails injected authentication principal
     * @return the newly created blog post with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new blog post (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponseDto>> createBlog(
            @Valid @RequestBody BlogCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String defaultAuthor = userDetails != null ? userDetails.getUsername() : "Admin";
        BlogResponseDto created = blogService.createBlog(request, defaultAuthor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Blog post created successfully"));
    }

    /**
     * Updates an existing blog post by ID. Restricted to administrators.
     *
     * @param id blog post primary key
     * @param request validated update payload
     * @return the updated blog post details
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing blog post (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BlogResponseDto>> updateBlog(
            @PathVariable Long id,
            @Valid @RequestBody BlogUpdateRequest request
    ) {
        BlogResponseDto updated = blogService.updateBlog(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Blog post updated successfully"));
    }

    /**
     * Permanently deletes a blog post by ID. Restricted to administrators.
     *
     * @param id blog post primary key
     * @return empty response body on success
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a blog post (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id) {
        blogService.deleteBlog(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Blog post deleted successfully"));
    }
}

