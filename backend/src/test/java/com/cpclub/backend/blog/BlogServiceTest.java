package com.cpclub.backend.blog;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogEditDto;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.dto.BlogUpdateRequest;
import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;
import com.cpclub.backend.blog.repository.BlogCommentRepository;
import com.cpclub.backend.blog.repository.BlogRepository;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.blog.service.BlogService.Caller;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The post workflow of Section 8.1: who may do what, from which status. */
class BlogServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("Asia/Kolkata"));

    private BlogRepository blogRepository;
    private BlogCommentRepository commentRepository;
    private UserRepository userRepository;

    private final User alice = User.builder().id(1L).name("Alice").email("alice@dau.ac.in").build();
    private final User bob = User.builder().id(2L).name("Bob").email("bob@dau.ac.in").build();
    private final User admin = User.builder().id(9L).name("Admin").email("admin@dau.ac.in").build();

    private final Caller asAlice = new Caller(1L, false);
    private final Caller asBob = new Caller(2L, false);
    private final Caller asAdmin = new Caller(9L, true);

    @BeforeEach
    void setUp() {
        blogRepository = mock(BlogRepository.class);
        commentRepository = mock(BlogCommentRepository.class);
        userRepository = mock(UserRepository.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(userRepository.findById(9L)).thenReturn(Optional.of(admin));
        when(blogRepository.save(any(BlogPost.class))).thenAnswer(i -> i.getArgument(0));
    }

    private BlogService service() {
        return service(false);
    }

    private BlogService service(boolean requireVerification) {
        return new BlogService(blogRepository, commentRepository, userRepository,
                new VerifiedMemberGuard(requireVerification), CLOCK);
    }

    // ------------------------------------------------------------- public reads

    @Test
    @DisplayName("Published posts come back newest-published first")
    void publishedListIsPublishedOnly() {
        when(blogRepository.findByStatus(eq(BlogStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post(5L, alice, BlogStatus.PUBLISHED)), PageRequest.of(0, 10), 1));

        PagedResponse<BlogResponseDto> page = service().getPublishedBlogs(0, 10);

        assertEquals(1, page.content().size());
        ArgumentCaptor<Pageable> asked = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).findByStatus(eq(BlogStatus.PUBLISHED), asked.capture());
        assertEquals("publishedAt: DESC", asked.getValue().getSort().iterator().next().toString());
    }

    @Test
    @DisplayName("Anonymous GET of a draft by id is a 404")
    void draftById_is404() {
        when(blogRepository.findByIdAndStatus(5L, BlogStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service().getBlogById(5L));
    }

    @Test
    @DisplayName("A draft is not readable by guessing its slug either")
    void draftBySlug_is404() {
        when(blogRepository.findBySlugAndStatus("secret-draft", BlogStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service().getBlogBySlug("secret-draft"));
    }

    @Test
    @DisplayName("Read time is words / 200, rounded, never below one minute")
    void readMinutes() {
        BlogPost p = post(5L, alice, BlogStatus.PUBLISHED);
        p.setContent("word ".repeat(700));
        when(blogRepository.findByIdAndStatus(5L, BlogStatus.PUBLISHED)).thenReturn(Optional.of(p));

        assertEquals(4, service().getBlogById(5L).readMinutes(), "700 / 200 = 3.5, rounds to 4");
    }

    // ------------------------------------------------------------- authoring

    @Test
    @DisplayName("A member's new post is a draft credited to them, whatever they ask for")
    void memberCreatesADraft() {
        when(blogRepository.existsBySlug(anyString())).thenReturn(false);

        BlogResponseDto created = service().create(
                new BlogCreateRequest("Intro to DP", "Body text", "Someone Famous", "dp", true, true), asAlice);

        assertEquals(BlogStatus.DRAFT, created.status(), "publishNow and published are admin-only");
        assertEquals("Alice", created.authorName(), "authorName is admin-only");
        assertEquals(1L, created.authorId());
        assertEquals("intro-to-dp", created.slug());
    }

    @Test
    @DisplayName("A clashing slug gets -2, then -3")
    void slugClashGetsANumber() {
        when(blogRepository.existsBySlug("intro-to-dp")).thenReturn(true);
        when(blogRepository.existsBySlug("intro-to-dp-2")).thenReturn(true);
        when(blogRepository.existsBySlug("intro-to-dp-3")).thenReturn(false);

        BlogResponseDto created = service().create(
                new BlogCreateRequest("Intro to DP", "Body", null, null, null, null), asAlice);

        assertEquals("intro-to-dp-3", created.slug());
    }

    @Test
    @DisplayName("An admin may publish immediately")
    void adminPublishesNow() {
        BlogResponseDto created = service().create(
                new BlogCreateRequest("Club news", "Body", null, null, null, true), asAdmin);

        assertEquals(BlogStatus.PUBLISHED, created.status());
        assertEquals(LocalDateTime.of(2026, 10, 15, 12, 0), created.publishedAt(), "UTC (D31)");
    }

    @Test
    @DisplayName("An unverified member cannot write when verification is required")
    void unverifiedMember_is403() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service(true).create(
                new BlogCreateRequest("Title", "Body", null, null, null, null), asAlice));

        assertEquals(403, e.getStatusCode().value());
        assertEquals("EMAIL_NOT_VERIFIED", e.getReason());
        verify(blogRepository, never()).save(any());
    }

    @Test
    @DisplayName("The author submits a draft: PENDING_REVIEW, submitted_at set")
    void submitDraft() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.DRAFT)));

        BlogEditDto result = service().submit(5L, asAlice);

        assertEquals(BlogStatus.PENDING_REVIEW, result.status());
        assertNotNull(result.submittedAt());
    }

    @Test
    @DisplayName("The author cannot edit a post that is under review")
    void editWhilePending_is409() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.PENDING_REVIEW)));

        ConflictException e = assertThrows(ConflictException.class,
                () -> service().update(5L, new BlogUpdateRequest("New title", "New body", null, null), asAlice));
        assertTrue(e.getMessage().contains("PENDING_REVIEW"), "the refusal names the status");
    }

    @Test
    @DisplayName("An admin edit keeps a published post published")
    void adminEditKeepsStatus() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.PUBLISHED)));

        BlogEditDto result = service().update(5L, new BlogUpdateRequest("Fixed typo", "Body", null, false), asAdmin);

        assertEquals(BlogStatus.PUBLISHED, result.status(), "the legacy published flag is ignored");
        assertEquals("Fixed typo", result.title());
    }

    @Test
    @DisplayName("Rejecting without a note is a 400")
    void rejectWithoutNote_is400() {
        assertThrows(BadRequestException.class, () -> service().reject(5L, "   ", asAdmin));
    }

    @Test
    @DisplayName("Resubmitting a rejected post puts it back in review and clears the note")
    void resubmitRejected() {
        BlogPost rejected = post(5L, alice, BlogStatus.REJECTED);
        rejected.setReviewNote("Add an example for the recurrence");
        when(blogRepository.findById(5L)).thenReturn(Optional.of(rejected));

        BlogEditDto result = service().submit(5L, asAlice);

        assertEquals(BlogStatus.PENDING_REVIEW, result.status());
        assertNull(result.reviewNote());
    }

    @Test
    @DisplayName("Another member asking to edit someone's draft gets 404, not 403")
    void strangerGetForEditing_is404() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.DRAFT)));

        // 403 would confirm the draft exists.
        assertThrows(ResourceNotFoundException.class, () -> service().getForEditing(5L, asBob));
    }

    @Test
    @DisplayName("An admin cannot publish someone else's draft that was never submitted")
    void publishSomeoneElsesDraft_is409() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.DRAFT)));

        assertThrows(ConflictException.class, () -> service().publish(5L, asAdmin));
    }

    @Test
    @DisplayName("Publishing records the reviewer and the publish time")
    void publishFromReview() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.PENDING_REVIEW)));

        BlogEditDto result = service().publish(5L, asAdmin);

        assertEquals(BlogStatus.PUBLISHED, result.status());
        assertEquals(LocalDateTime.of(2026, 10, 15, 12, 0), result.publishedAt());
    }

    @Test
    @DisplayName("An author cannot delete a published post; an admin can")
    void deleteRules() {
        when(blogRepository.findById(5L)).thenReturn(Optional.of(post(5L, alice, BlogStatus.PUBLISHED)));

        assertThrows(ConflictException.class, () -> service().delete(5L, asAlice));
        service().delete(5L, asAdmin);
        verify(commentRepository).deleteAllByPostId(5L);
        verify(blogRepository).delete(any(BlogPost.class));
    }

    private static BlogPost post(Long id, User author, BlogStatus status) {
        return BlogPost.builder()
                .id(id).title("Post " + id).slug("post-" + id).content("Some content here")
                .author(author).authorName(author.getName()).status(status)
                .createdAt(LocalDateTime.of(2026, 10, 1, 10, 0))
                .build();
    }
}
