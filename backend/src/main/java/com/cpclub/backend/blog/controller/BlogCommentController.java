package com.cpclub.backend.blog.controller;

import com.cpclub.backend.blog.dto.CommentDto;
import com.cpclub.backend.blog.dto.CommentRequest;
import com.cpclub.backend.blog.service.BlogCommentService;
import com.cpclub.backend.common.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Comments on published posts (D19). Reading is public; writing needs a login. */
@RestController
@RequiredArgsConstructor
@Tag(name = "Blog comments", description = "Plain-text comments with one level of replies")
public class BlogCommentController {

    private final BlogCommentService commentService;

    /** Public. A signed-in reader also gets canEdit and canDelete worked out for them. */
    @GetMapping("/api/blogs/{id:[0-9]+}/comments")
    @Operation(summary = "The comment thread on a published post")
    public ResponseEntity<ApiResponse<List<CommentDto>>> list(
            @PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(commentService.list(id, BlogController.caller(me)),
                "Fetched comments successfully"));
    }

    @PostMapping("/api/blogs/{id:[0-9]+}/comments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Comment on a published post, or reply to a comment")
    public ResponseEntity<ApiResponse<CommentDto>> create(
            @PathVariable Long id, @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                commentService.create(id, request, BlogController.caller(me)), "Comment posted"));
    }

    @PutMapping("/api/comments/{id:[0-9]+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Edit your comment, within 15 minutes of posting it")
    public ResponseEntity<ApiResponse<CommentDto>> edit(
            @PathVariable Long id, @Valid @RequestBody CommentRequest request,
            @AuthenticationPrincipal UserDetailsImpl me) {
        return ResponseEntity.ok(ApiResponse.success(
                commentService.edit(id, request, BlogController.caller(me)), "Comment updated"));
    }

    @DeleteMapping("/api/comments/{id:[0-9]+}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a comment: your own, or any as an admin")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl me) {
        commentService.delete(id, BlogController.caller(me));
        return ResponseEntity.ok(ApiResponse.success(null, "Comment deleted"));
    }
}
