package com.cpclub.backend.blog.repository;

import com.cpclub.backend.blog.entity.BlogPost;
import com.cpclub.backend.blog.entity.BlogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Blog posts.
 *
 * <p>Every public lookup takes a status, and the public endpoints only ever pass
 * PUBLISHED. There is deliberately no unfiltered find-by-slug here: an unfiltered
 * lookup on a public route is how anyone walks ids or guesses slugs to read drafts.</p>
 */
@Repository
public interface BlogRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findByIdAndStatus(Long id, BlogStatus status);

    Optional<BlogPost> findBySlugAndStatus(String slug, BlogStatus status);

    Page<BlogPost> findByStatus(BlogStatus status, Pageable pageable);

    boolean existsBySlug(String slug);

    /** The caller's own posts, every status: "my posts". */
    List<BlogPost> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);

    /** The review queue: oldest submission first, so nothing waits behind newer posts. */
    List<BlogPost> findByStatusOrderBySubmittedAtAsc(BlogStatus status);
}
