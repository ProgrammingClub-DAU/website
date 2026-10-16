package com.cpclub.backend.blog.controller;

import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogEditDto;
import com.cpclub.backend.blog.dto.BlogRejectRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.dto.BlogUpdateRequest;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.blog.service.BlogService.Caller;
import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.security.service.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Blog posts and their review workflow (D18).
 *
 * <p>SecurityConfig makes every GET under {@code /api/blogs/**} public, and matches
 * {@code /mine} and {@code /review-queue} ahead of that. {@code /{id}/edit} is not
 * matched ahead of it, so its {@code @PreAuthorize} is the only thing requiring a
 * login there. Numeric routes are {@code /{id:[0-9]+}} so that {@code /mine} and
 * {@code /review-queue} can never be read as an id.</p>
 *
 * <p>Who may edit or delete a post depends on who wrote it, which the URL layer cannot
 * know; those checks live in BlogService, not here.</p>
 */
@RestController
@RequestMapping("/api/blogs")
@RequiredArgsConstructor
@Tag(name = "Blogs & Editorial CMS", description = "Member-written posts with core-team review")
public class BlogController {

    private final BlogService blogService;

    // ------------------------------------------------------------------ public

    @GetMapping
    @Operation(summary = "Published posts, newest first (paginated)")
    public ResponseEntity<ApiResponse<PagedResponse<BlogResponseDto>>> getPublishedBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getPublishedBlogs(page, size),
                "Fetched blog posts successfully"));
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "A published post by id")
    public ResponseEntity<ApiResponse<BlogResponseDto>> getBlogById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getBlogById(id), "Fetched blog post successfully"));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "A published post by slug")
    public ResponseEntity<ApiResponse<BlogResponseDto>> getBlogBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getBlogBySlug(slug), "Fetched blog post successfully"));
    }

    // ------------------------------------------------------------------ authoring

    @GetMapping("/{id:[0-9]+}/edit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "A post with its review note, for its author or an admin")
    public ResponseEntity<ApiResponse<BlogEditDto>> getForEditing(
            @PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.getForEditing(id, caller(me)), "Fetched blog post successfully"));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "The caller's posts, every status")
    public ResponseEntity<ApiResponse<List<BlogEditDto>>> mine(@AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.mine(caller(me)), "Fetched your posts successfully"));
    }

    @GetMapping("/review-queue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Posts waiting for review, oldest first (admin)")
    public ResponseEntity<ApiResponse<List<BlogEditDto>>> reviewQueue(@AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.reviewQueue(caller(me)), "Fetched review queue successfully"));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Write a post; it starts as a draft")
    public ResponseEntity<ApiResponse<BlogResponseDto>> createBlog(
            @Valid @RequestBody BlogCreateRequest request, @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(blogService.create(request, caller(me)), "Blog post created successfully"));
    }

    @PutMapping("/{id:[0-9]+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Edit a post: its author while DRAFT or REJECTED, or an admin")
    public ResponseEntity<ApiResponse<BlogEditDto>> updateBlog(
            @PathVariable Long id, @Valid @RequestBody BlogUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.update(id, request, caller(me)), "Blog post updated successfully"));
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a post: its author while DRAFT or REJECTED, or an admin")
    public ResponseEntity<ApiResponse<Void>> deleteBlog(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl me) {
        blogService.delete(id, caller(me));
        return ResponseEntity.ok(ApiResponse.success(null, "Blog post deleted successfully"));
    }

    @PostMapping("/{id:[0-9]+}/submit")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Submit a draft for review")
    public ResponseEntity<ApiResponse<BlogEditDto>> submit(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.submit(id, caller(me)), "Submitted for review"));
    }

    @PostMapping("/{id:[0-9]+}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish a post (admin)")
    public ResponseEntity<ApiResponse<BlogEditDto>> publish(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.publish(id, caller(me)), "Published"));
    }

    @PostMapping("/{id:[0-9]+}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Send a post back to its author with a note (admin)")
    public ResponseEntity<ApiResponse<BlogEditDto>> reject(
            @PathVariable Long id, @Valid @RequestBody BlogRejectRequest request,
            @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(blogService.reject(id, request.note(), caller(me)), "Rejected"));
    }

    static Caller caller(UserDetailsImpl me) {
        if (me == null) {
            return null;
        }
        boolean admin = me.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return new Caller(me.getId(), admin);
    }
}
