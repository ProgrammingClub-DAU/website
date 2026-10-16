package com.cpclub.backend.blog.service;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.blog.dto.CommentDto;
import com.cpclub.backend.blog.dto.CommentRequest;
import com.cpclub.backend.blog.entity.BlogComment;
import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;
import com.cpclub.backend.blog.repository.BlogCommentRepository;
import com.cpclub.backend.blog.repository.BlogRepository;
import com.cpclub.backend.blog.service.BlogService.Caller;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.common.exception.TooManyRequestsException;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comments on published posts (D19): deliberately small.
 *
 * <ul>
 *   <li>Plain text, stored exactly as submitted; the frontend renders it as text.</li>
 *   <li>One level of replies. A reply to a reply is refused rather than flattened,
 *       so nobody's reply silently lands under the wrong comment.</li>
 *   <li>Editable by its author for 15 minutes; deletable by its author or an admin
 *       at any time, as a soft delete.</li>
 *   <li>One comment per 20 seconds per member.</li>
 * </ul>
 *
 * <p>The rate limit is held in memory, as D30 allows for a single instance. It is a
 * brake on accidents and floods, not a security boundary, so losing it on restart
 * costs nothing.</p>
 *
 * <p><b>Times.</b> {@code created_at} is stamped by Hibernate from the JVM clock in the
 * JVM's zone, so the 15-minute window is measured against the same zone. On Render
 * that zone is UTC, which is also what D31 asks for.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogCommentService {

    static final Duration EDIT_WINDOW = Duration.ofMinutes(15);
    static final Duration MIN_GAP = Duration.ofSeconds(20);
    static final int MAX_LENGTH = 2000;

    private final BlogCommentRepository commentRepository;
    private final BlogRepository blogRepository;
    private final UserRepository userRepository;
    private final VerifiedMemberGuard verifiedMemberGuard;
    private final Clock clock;

    private final Map<Long, Instant> lastCommentAt = new ConcurrentHashMap<>();

    /**
     * The thread on a published post. Top-level comments oldest first, each with its
     * replies. A deleted comment that has replies keeps its place, body hidden, so the
     * replies still read as answers to something; one with no replies is left out.
     *
     * @param caller null for an anonymous reader
     */
    @Transactional(readOnly = true)
    public List<CommentDto> list(Long postId, Caller caller) {
        requirePublished(postId);
        Map<Long, List<BlogComment>> replies = new LinkedHashMap<>();
        List<BlogComment> topLevel = new ArrayList<>();
        for (BlogComment c : commentRepository.findThread(postId)) {
            if (c.getParent() == null) {
                topLevel.add(c);
            } else {
                replies.computeIfAbsent(c.getParent().getId(), k -> new ArrayList<>()).add(c);
            }
        }

        List<CommentDto> thread = new ArrayList<>();
        for (BlogComment top : topLevel) {
            List<CommentDto> answers = replies.getOrDefault(top.getId(), List.of()).stream()
                    .filter(r -> !r.isDeleted())
                    .map(r -> toDto(r, caller, List.of()))
                    .toList();
            if (top.isDeleted() && answers.isEmpty()) {
                continue;
            }
            thread.add(toDto(top, caller, answers));
        }
        return thread;
    }

    @Transactional
    public CommentDto create(Long postId, CommentRequest request, Caller caller) {
        User author = userRepository.findById(caller.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + caller.userId()));
        verifiedMemberGuard.require(author);
        BlogPost post = requirePublished(postId);
        String body = validBody(request.body());

        BlogComment parent = null;
        if (request.parentId() != null) {
            parent = commentRepository.findById(request.parentId())
                    .filter(p -> p.getPost().getId().equals(postId))
                    .orElseThrow(() -> new BadRequestException("You can only reply to a comment on this post"));
            if (parent.getParent() != null) {
                throw new BadRequestException("Replies are one level deep: reply to the comment above instead");
            }
        }

        claimCommentSlot(caller.userId());
        BlogComment saved = commentRepository.save(BlogComment.builder()
                .post(post).user(author).parent(parent).body(body).build());
        log.info("User {} commented {} on post {}", caller.userId(), saved.getId(), postId);
        return toDto(saved, caller, List.of());
    }

    /** The author only, within 15 minutes, on a comment that is not deleted. */
    @Transactional
    public CommentDto edit(Long commentId, CommentRequest request, Caller caller) {
        BlogComment comment = requireComment(commentId);
        if (!isOwner(comment, caller) || comment.isDeleted() || !withinEditWindow(comment)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Comments can be edited by their author for 15 minutes after posting");
        }
        comment.setBody(validBody(request.body()));
        comment.setUpdatedAt(nowJvm());
        return toDto(commentRepository.save(comment), caller, List.of());
    }

    /** The author or an admin. A soft delete; idempotent. */
    @Transactional
    public void delete(Long commentId, Caller caller) {
        BlogComment comment = requireComment(commentId);
        if (!isOwner(comment, caller) && !caller.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or an admin can delete a comment");
        }
        if (!comment.isDeleted()) {
            comment.setDeletedAt(nowJvm());
            commentRepository.save(comment);
            log.info("Comment {} deleted by user {}", commentId, caller.userId());
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Records this comment's time if the member is clear to post, atomically, so two
     * requests in the same instant cannot both get through.
     */
    private void claimCommentSlot(Long userId) {
        Instant now = clock.instant();
        long[] wait = {0};
        lastCommentAt.compute(userId, (id, previous) -> {
            if (previous != null && previous.plus(MIN_GAP).isAfter(now)) {
                wait[0] = Duration.between(now, previous.plus(MIN_GAP)).toMillis();
                return previous;
            }
            return now;
        });
        if (wait[0] > 0) {
            long seconds = (wait[0] + 999) / 1000;
            throw new TooManyRequestsException("You can comment again in " + seconds + " seconds", seconds);
        }
    }

    private BlogPost requirePublished(Long postId) {
        // A draft's comments 404 like the draft itself does.
        return blogRepository.findByIdAndStatus(postId, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with id: " + postId));
    }

    private BlogComment requireComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
    }

    private static String validBody(String raw) {
        String body = raw == null ? "" : raw.trim();
        if (body.isEmpty() || body.length() > MAX_LENGTH) {
            throw new BadRequestException("A comment must be 1 to " + MAX_LENGTH + " characters");
        }
        return body;
    }

    private static boolean isOwner(BlogComment comment, Caller caller) {
        return caller != null && comment.getUser().getId().equals(caller.userId());
    }

    private boolean withinEditWindow(BlogComment comment) {
        return comment.getCreatedAt() != null && !nowJvm().isAfter(comment.getCreatedAt().plus(EDIT_WINDOW));
    }

    /** "Now" in the zone Hibernate stamped created_at in. */
    private LocalDateTime nowJvm() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.systemDefault());
    }

    private CommentDto toDto(BlogComment c, Caller caller, List<CommentDto> replies) {
        boolean deleted = c.isDeleted();
        User u = c.getUser();
        return new CommentDto(
                c.getId(),
                c.getParent() != null ? c.getParent().getId() : null,
                new CommentDto.Author(u.getId(), u.getName(), u.getAvatarUrl(), u.getCodeforcesHandle()),
                deleted ? null : c.getBody(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                deleted,
                !deleted && isOwner(c, caller) && withinEditWindow(c),
                !deleted && caller != null && (isOwner(c, caller) || caller.admin()),
                replies);
    }
}
