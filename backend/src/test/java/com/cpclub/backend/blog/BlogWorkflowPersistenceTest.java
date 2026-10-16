package com.cpclub.backend.blog;

import com.cpclub.backend.blog.dto.BlogCreateRequest;
import com.cpclub.backend.blog.dto.BlogResponseDto;
import com.cpclub.backend.blog.service.BlogService;
import com.cpclub.backend.blog.service.BlogService.Caller;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The workflow against the real schema, end to end.
 *
 * <p>The legacy {@code published} column is kept by V20 so the previous backend
 * release still reads correct values during a rolling deploy. The entity keeps it in
 * step with {@code status}; this reads it back with SQL to prove it.</p>
 */
@Transactional
class BlogWorkflowPersistenceTest extends PostgresIntegrationTest {

    @Autowired private BlogService blogService;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("Draft, submit, publish: the published column follows the status")
    void publishSetsTheLegacyColumn() {
        User author = userRepository.save(User.builder().name("Author").email("author-wf@dau.ac.in").build());
        User admin = userRepository.save(User.builder().name("Admin").email("admin-wf@dau.ac.in").role(Role.ROLE_ADMIN).build());
        Caller asAuthor = new Caller(author.getId(), false);
        Caller asAdmin = new Caller(admin.getId(), true);

        BlogResponseDto draft = blogService.create(new BlogCreateRequest("Segment trees", "Body", null, null, null, null), asAuthor);
        assertFalse(publishedColumn(draft.id()));
        assertThrows(ResourceNotFoundException.class, () -> blogService.getBlogBySlug("segment-trees"),
                "a draft is not public");

        blogService.submit(draft.id(), asAuthor);
        assertFalse(publishedColumn(draft.id()));

        blogService.publish(draft.id(), asAdmin);
        assertTrue(publishedColumn(draft.id()));
        assertEquals("PUBLISHED", jdbc.queryForObject("SELECT status FROM blog_posts WHERE id = ?", String.class, draft.id()));
        assertEquals(draft.id(), blogService.getBlogBySlug("segment-trees").id(), "now public");
    }

    private boolean publishedColumn(Long id) {
        // JDBC does not see pending JPA changes, and the @PreUpdate hook that keeps the
        // column in step only runs at flush -- so flush first, or this reads stale rows.
        entityManager.flush();
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT published FROM blog_posts WHERE id = ?", Boolean.class, id));
    }
}
