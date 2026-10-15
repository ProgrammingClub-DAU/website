package com.cpclub.backend.blog;

import com.cpclub.backend.auth.service.VerifiedMemberGuard;
import com.cpclub.backend.blog.dto.CommentDto;
import com.cpclub.backend.blog.dto.CommentRequest;
import com.cpclub.backend.blog.entity.BlogComment;
import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;
import com.cpclub.backend.blog.repository.BlogCommentRepository;
import com.cpclub.backend.blog.repository.BlogRepository;
import com.cpclub.backend.blog.service.BlogCommentService;
import com.cpclub.backend.blog.service.BlogService.Caller;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.common.exception.TooManyRequestsException;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The comment rules of D19 and Section 8.3. */
class BlogCommentServiceTest {

    /** "Now" for every test: 12:00 in the JVM's own zone, the zone created_at is stamped in. */
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 12, 0);

    private BlogCommentRepository commentRepository;
    private BlogRepository blogRepository;
    private UserRepository userRepository;
    private MutableClock clock;
    private BlogCommentService service;

    private final User alice = User.builder().id(1L).name("Alice").email("alice@dau.ac.in").build();
    private final User bob = User.builder().id(2L).name("Bob").email("bob@dau.ac.in").build();
    private final Caller asAlice = new Caller(1L, false);
    private final Caller asBob = new Caller(2L, false);
    private final Caller asAdmin = new Caller(9L, true);
    private BlogPost published;

    @BeforeEach
    void setUp() {
        commentRepository = mock(BlogCommentRepository.class);
        blogRepository = mock(BlogRepository.class);
        userRepository = mock(UserRepository.class);
        clock = new MutableClock(NOW.atZone(ZoneId.systemDefault()).toInstant());
        service = new BlogCommentService(commentRepository, blogRepository, userRepository,
                new VerifiedMemberGuard(false), clock);

        published = BlogPost.builder().id(5L).title("Post").slug("post").content("x")
                .authorName("Alice").status(BlogStatus.PUBLISHED).build();
        when(blogRepository.findByIdAndStatus(5L, BlogStatus.PUBLISHED)).thenReturn(Optional.of(published));
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));
        when(commentRepository.save(any(BlogComment.class))).thenAnswer(i -> {
            BlogComment c = i.getArgument(0);
            if (c.getId() == null) {
                c.setId(100L);
            }
            return c;
        });
    }

    @Test
    @DisplayName("A reply to a reply is refused")
    void replyToReply_is400() {
        BlogComment top = comment(10L, alice, null, NOW.minusMinutes(5));
        BlogComment reply = comment(11L, bob, top, NOW.minusMinutes(4));
        when(commentRepository.findById(11L)).thenReturn(Optional.of(reply));

        assertThrows(BadRequestException.class,
                () -> service.create(5L, new CommentRequest("Me too", 11L), asAlice));
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("A parent from a different post is refused")
    void parentOnAnotherPost_is400() {
        BlogPost other = BlogPost.builder().id(6L).build();
        BlogComment elsewhere = comment(12L, bob, null, NOW.minusMinutes(1));
        elsewhere.setPost(other);
        when(commentRepository.findById(12L)).thenReturn(Optional.of(elsewhere));

        assertThrows(BadRequestException.class,
                () -> service.create(5L, new CommentRequest("Hm", 12L), asAlice));
    }

    @Test
    @DisplayName("Commenting on a draft is a 404, as if the post were not there")
    void commentOnDraft_is404() {
        when(blogRepository.findByIdAndStatus(7L, BlogStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.create(7L, new CommentRequest("First!", null), asAlice));
    }

    @Test
    @DisplayName("Two comments within 20 seconds: the second is 429 with a wait time")
    void twoCommentsInTwentySeconds_is429() {
        service.create(5L, new CommentRequest("One", null), asAlice);
        clock.advanceSeconds(6);

        TooManyRequestsException e = assertThrows(TooManyRequestsException.class,
                () -> service.create(5L, new CommentRequest("Two", null), asAlice));

        assertEquals(429, e.getStatusCode().value());
        assertEquals(14, e.getRetryAfterSeconds());
        assertEquals("14", e.getHeaders().getFirst("Retry-After"));

        clock.advanceSeconds(14);
        service.create(5L, new CommentRequest("Two", null), asAlice);
    }

    @Test
    @DisplayName("The limit is per member: someone else may comment meanwhile")
    void limitIsPerMember() {
        service.create(5L, new CommentRequest("One", null), asAlice);
        service.create(5L, new CommentRequest("Also one", null), asBob);
    }

    @Test
    @DisplayName("A body that is empty once trimmed, or over 2000 characters, is refused")
    void bodyLimits() {
        assertThrows(BadRequestException.class, () -> service.create(5L, new CommentRequest("   ", null), asAlice));
        assertThrows(BadRequestException.class,
                () -> service.create(5L, new CommentRequest("x".repeat(2001), null), asAlice));
    }

    @Test
    @DisplayName("Editing after 16 minutes is a 403")
    void editAfterSixteenMinutes_is403() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, alice, null, NOW.minusMinutes(16))));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.edit(10L, new CommentRequest("Fixed", null), asAlice));
        assertEquals(403, e.getStatusCode().value());
    }

    @Test
    @DisplayName("Editing within 15 minutes works, and marks it edited")
    void editWithinWindow() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, alice, null, NOW.minusMinutes(14))));

        CommentDto edited = service.edit(10L, new CommentRequest("Fixed", null), asAlice);

        assertEquals("Fixed", edited.body());
        assertEquals(NOW, edited.editedAt());
    }

    @Test
    @DisplayName("Nobody edits someone else's comment, not even an admin")
    void editSomeoneElses_is403() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment(10L, alice, null, NOW.minusMinutes(1))));

        assertThrows(ResponseStatusException.class, () -> service.edit(10L, new CommentRequest("x", null), asBob));
        assertThrows(ResponseStatusException.class, () -> service.edit(10L, new CommentRequest("x", null), asAdmin));
    }

    @Test
    @DisplayName("An admin may delete anyone's comment; another member may not")
    void deleteRules() {
        BlogComment c = comment(10L, alice, null, NOW.minusMinutes(1));
        when(commentRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThrows(ResponseStatusException.class, () -> service.delete(10L, asBob));
        service.delete(10L, asAdmin);
        assertTrue(c.isDeleted(), "a soft delete");
    }

    @Test
    @DisplayName("A deleted comment with replies keeps its place, body hidden; one without replies is left out")
    void deletedCommentsInTheThread() {
        BlogComment withReplies = comment(10L, alice, null, NOW.minusMinutes(30));
        withReplies.setDeletedAt(NOW.minusMinutes(1));
        BlogComment reply = comment(11L, bob, withReplies, NOW.minusMinutes(20));
        BlogComment lonely = comment(12L, bob, null, NOW.minusMinutes(10));
        lonely.setDeletedAt(NOW.minusMinutes(2));
        BlogComment alive = comment(13L, bob, null, NOW.minusMinutes(5));
        when(commentRepository.findThread(5L)).thenReturn(List.of(withReplies, reply, lonely, alive));

        List<CommentDto> thread = service.list(5L, null);

        assertEquals(List.of(10L, 13L), thread.stream().map(CommentDto::id).toList());
        CommentDto kept = thread.get(0);
        assertTrue(kept.deleted());
        assertNull(kept.body(), "the text of a deleted comment is never served");
        assertEquals(1, kept.replies().size());
        assertEquals("Body 11", kept.replies().get(0).body());
    }

    @Test
    @DisplayName("canEdit and canDelete are worked out for the caller")
    void permissionsForTheCaller() {
        when(commentRepository.findThread(5L)).thenReturn(List.of(comment(10L, alice, null, NOW.minusMinutes(1))));

        CommentDto mine = service.list(5L, asAlice).get(0);
        CommentDto theirs = service.list(5L, asBob).get(0);
        CommentDto anonymous = service.list(5L, null).get(0);

        assertTrue(mine.canEdit());
        assertTrue(mine.canDelete());
        assertFalse(theirs.canEdit());
        assertFalse(theirs.canDelete());
        assertFalse(anonymous.canDelete());
    }

    private BlogComment comment(Long id, User author, BlogComment parent, LocalDateTime createdAt) {
        return BlogComment.builder().id(id).post(published).user(author).parent(parent)
                .body("Body " + id).createdAt(createdAt).build();
    }

    /** A clock tests can move forward. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advanceSeconds(long s) {
            now = now.plusSeconds(s);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
