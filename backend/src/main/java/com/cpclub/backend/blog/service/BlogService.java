package com.cpclub.backend.blog.service;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogEditDto;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.dto.BlogUpdateRequest;
import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;
import com.cpclub.backend.blog.repository.BlogCommentRepository;
import com.cpclub.backend.blog.repository.BlogRepository;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Blog posts and their review workflow (D18).
 *
 * <pre>
 *   DRAFT ──submit──▶ PENDING_REVIEW ──publish──▶ PUBLISHED
 *     ▲                     │
 *     └─────submit──── REJECTED ◀──reject──┘
 * </pre>
 *
 * <p>Members write; only an admin publishes. Every transition not in the table in
 * Section 8.1 is refused with 409, naming the post's current status, so an author
 * who double-submits learns what actually happened.</p>
 *
 * <p><b>What a stranger can learn.</b> Public reads see PUBLISHED posts only, by id or
 * by slug, and a post that is not yours and not published answers 404 to every
 * author-facing operation too -- never 403. A 403 would confirm that a draft with that
 * id exists.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlogService {

    private static final Set<BlogStatus> AUTHOR_EDITABLE = EnumSet.of(BlogStatus.DRAFT, BlogStatus.REJECTED);

    private final BlogRepository blogRepository;
    private final BlogCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final VerifiedMemberGuard verifiedMemberGuard;
    private final Clock clock;

    /** Who is asking. Resolved from the security principal by the controller. */
    public record Caller(Long userId, boolean admin) {
    }

    // ------------------------------------------------------------------ public reads

    @Transactional(readOnly = true)
    public PagedResponse<BlogResponseDto> getPublishedBlogs(int page, int size) {
        Page<BlogPost> posts = blogRepository.findByStatus(BlogStatus.PUBLISHED,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))));
        Map<Long, Long> counts = commentCounts(posts.getContent());
        return new PagedResponse<>(
                posts.getContent().stream()
                        .map(p -> BlogResponseDto.fromEntity(p, counts.getOrDefault(p.getId(), 0L)))
                        .toList(),
                posts.getNumber(),
                posts.getSize(),
                posts.getTotalElements(),
                posts.getTotalPages(),
                posts.isLast());
    }

    @Transactional(readOnly = true)
    public BlogResponseDto getBlogById(Long id) {
        // Published-only: this endpoint is public, and an unfiltered lookup lets
        // anyone walk ids to read unpublished drafts.
        BlogPost post = blogRepository.findByIdAndStatus(id, BlogStatus.PUBLISHED)
                .orElseThrow(() -> notFound(id));
        return BlogResponseDto.fromEntity(post, commentRepository.countByPostIdAndDeletedAtIsNull(id));
    }

    @Transactional(readOnly = true)
    public BlogResponseDto getBlogBySlug(String slug) {
        // Published-only, for the same reason as getBlogById: slugs are guessable.
        BlogPost post = blogRepository.findBySlugAndStatus(slug, BlogStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("Blog post not found with slug: " + slug));
        return BlogResponseDto.fromEntity(post, commentRepository.countByPostIdAndDeletedAtIsNull(post.getId()));
    }

    // ------------------------------------------------------------------ authoring

    /**
     * A new post: a DRAFT credited to the caller. An admin may publish it at once, and
     * may credit it to someone else by name.
     */
    @Transactional
    public BlogResponseDto create(BlogCreateRequest request, Caller caller) {
        User author = requireUser(caller.userId());
        verifiedMemberGuard.require(author);

        boolean publishNow = caller.admin()
                && (Boolean.TRUE.equals(request.publishNow()) || Boolean.TRUE.equals(request.published()));
        String authorName = caller.admin() && request.authorName() != null && !request.authorName().isBlank()
                ? request.authorName().trim()
                : author.getName();

        BlogPost post = BlogPost.builder()
                .title(request.title().trim())
                .slug(uniqueSlug(request.title()))
                .content(request.content())
                .tags(request.tags())
                .author(author)
                .authorName(authorName)
                .status(publishNow ? BlogStatus.PUBLISHED : BlogStatus.DRAFT)
                .reviewer(publishNow ? author : null)
                .publishedAt(publishNow ? nowUtc() : null)
                .build();
        BlogPost saved = blogRepository.save(post);
        log.info("User {} created blog post {} as {}", caller.userId(), saved.getId(), saved.getStatus());
        return BlogResponseDto.fromEntity(saved, 0);
    }

    /**
     * New content. Status never changes here. An author may edit only a DRAFT or a
     * REJECTED post -- once submitted, the reviewer is looking at a fixed text. An
     * admin may edit any post, and a PUBLISHED one stays published.
     */
    @Transactional
    public BlogEditDto update(Long id, BlogUpdateRequest request, Caller caller) {
        BlogPost post = requireVisibleTo(id, caller);
        if (!caller.admin() && !AUTHOR_EDITABLE.contains(post.getStatus())) {
            throw conflict(post, "edited");
        }
        post.setTitle(request.title().trim());
        post.setContent(request.content());
        if (request.tags() != null) {
            post.setTags(request.tags());
        }
        BlogPost saved = blogRepository.save(post);
        log.info("User {} updated blog post {}", caller.userId(), id);
        return BlogEditDto.fromEntity(saved, commentRepository.countByPostIdAndDeletedAtIsNull(id));
    }

    /** DRAFT or REJECTED to PENDING_REVIEW. The author's own posts only. */
    @Transactional
    public BlogEditDto submit(Long id, Caller caller) {
        BlogPost post = requireOwnedBy(id, caller);
        if (!AUTHOR_EDITABLE.contains(post.getStatus())) {
            throw conflict(post, "submitted");
        }
        post.setStatus(BlogStatus.PENDING_REVIEW);
        post.setSubmittedAt(nowUtc());
        // A resubmission starts a fresh review: the last reviewer's note is spent.
        post.setReviewNote(null);
        log.info("Blog post {} submitted for review", id);
        return BlogEditDto.fromEntity(blogRepository.save(post), 0);
    }

    /** PENDING_REVIEW to PUBLISHED -- or an admin publishing their own draft. Admin only. */
    @Transactional
    public BlogEditDto publish(Long id, Caller caller) {
        requireAdmin(caller);
        BlogPost post = blogRepository.findById(id).orElseThrow(() -> notFound(id));
        boolean ownDraft = post.getStatus() == BlogStatus.DRAFT && isAuthor(post, caller);
        if (post.getStatus() != BlogStatus.PENDING_REVIEW && !ownDraft) {
            throw conflict(post, "published");
        }
        post.setStatus(BlogStatus.PUBLISHED);
        post.setReviewer(requireUser(caller.userId()));
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(nowUtc());
        }
        log.info("Admin {} published blog post {}", caller.userId(), id);
        return BlogEditDto.fromEntity(blogRepository.save(post), 0);
    }

    /** PENDING_REVIEW to REJECTED, with a note the author will see. Admin only. */
    @Transactional
    public BlogEditDto reject(Long id, String note, Caller caller) {
        requireAdmin(caller);
        String trimmed = note == null ? "" : note.trim();
        if (trimmed.isEmpty() || trimmed.length() > 500) {
            throw new BadRequestException("A rejection needs a note for the author, 1 to 500 characters");
        }
        BlogPost post = blogRepository.findById(id).orElseThrow(() -> notFound(id));
        if (post.getStatus() != BlogStatus.PENDING_REVIEW) {
            throw conflict(post, "rejected");
        }
        post.setStatus(BlogStatus.REJECTED);
        post.setReviewer(requireUser(caller.userId()));
        post.setReviewNote(trimmed);
        log.info("Admin {} rejected blog post {}", caller.userId(), id);
        return BlogEditDto.fromEntity(blogRepository.save(post), 0);
    }

    /** An author may delete their own DRAFT or REJECTED post; an admin, any post. */
    @Transactional
    public void delete(Long id, Caller caller) {
        BlogPost post = requireVisibleTo(id, caller);
        if (!caller.admin() && !AUTHOR_EDITABLE.contains(post.getStatus())) {
            throw conflict(post, "deleted");
        }
        // Comments first. Replies before the comments they answer, since the H2 test
        // schema has no ON DELETE CASCADE to do it.
        commentRepository.deleteRepliesByPostId(id);
        commentRepository.deleteAllByPostId(id);
        blogRepository.delete(post);
        log.info("User {} deleted blog post {}", caller.userId(), id);
    }

    /** The full post, review note included, for its author or an admin. */
    @Transactional(readOnly = true)
    public BlogEditDto getForEditing(Long id, Caller caller) {
        BlogPost post = requireVisibleTo(id, caller);
        return BlogEditDto.fromEntity(post, commentRepository.countByPostIdAndDeletedAtIsNull(id));
    }

    @Transactional(readOnly = true)
    public List<BlogEditDto> mine(Caller caller) {
        List<BlogPost> posts = blogRepository.findByAuthorIdOrderByUpdatedAtDesc(caller.userId());
        Map<Long, Long> counts = commentCounts(posts);
        return posts.stream().map(p -> BlogEditDto.fromEntity(p, counts.getOrDefault(p.getId(), 0L))).toList();
    }

    @Transactional(readOnly = true)
    public List<BlogEditDto> reviewQueue(Caller caller) {
        requireAdmin(caller);
        return blogRepository.findByStatusOrderBySubmittedAtAsc(BlogStatus.PENDING_REVIEW).stream()
                .map(p -> BlogEditDto.fromEntity(p, 0))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    /**
     * The post, if the caller is its author or an admin. Anyone else gets the same
     * 404 a missing post gives, so the answer never confirms a draft exists.
     */
    private BlogPost requireVisibleTo(Long id, Caller caller) {
        BlogPost post = blogRepository.findById(id).orElseThrow(() -> notFound(id));
        if (!caller.admin() && !isAuthor(post, caller)) {
            throw notFound(id);
        }
        return post;
    }

    /** As {@link #requireVisibleTo}, but admins do not get a pass: submitting is the author's act. */
    private BlogPost requireOwnedBy(Long id, Caller caller) {
        BlogPost post = blogRepository.findById(id).orElseThrow(() -> notFound(id));
        if (!isAuthor(post, caller)) {
            throw notFound(id);
        }
        return post;
    }

    private static boolean isAuthor(BlogPost post, Caller caller) {
        return post.getAuthor() != null && post.getAuthor().getId().equals(caller.userId());
    }

    /** Belt and braces: the URL rules already restrict these to admins. */
    private static void requireAdmin(Caller caller) {
        if (!caller.admin()) {
            throw new org.springframework.security.access.AccessDeniedException("Admins only");
        }
    }

    private static ConflictException conflict(BlogPost post, String verb) {
        return new ConflictException("This post is " + post.getStatus() + " and cannot be " + verb + " now");
    }

    private Map<Long, Long> commentCounts(List<BlogPost> posts) {
        Map<Long, Long> counts = new HashMap<>();
        if (!posts.isEmpty()) {
            for (Object[] row : commentRepository.countVisibleByPostIds(posts.stream().map(BlogPost::getId).toList())) {
                counts.put((Long) row[0], (Long) row[1]);
            }
        }
        return counts;
    }

    /** The title's slug, with -2, -3 ... appended until it is free. */
    private String uniqueSlug(String title) {
        String base = generateSlug(title);
        if (base.isEmpty()) {
            base = "post";
        }
        String slug = base;
        for (int n = 2; blogRepository.existsBySlug(slug); n++) {
            slug = base + "-" + n;
        }
        return slug;
    }

    static String generateSlug(String title) {
        return title.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    private LocalDateTime nowUtc() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private static ResourceNotFoundException notFound(Long id) {
        return new ResourceNotFoundException("Blog post not found with id: " + id);
    }
}
