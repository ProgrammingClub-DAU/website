package com.cpclub.backend.blog.repository;

import com.cpclub.backend.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * JPA repository for {@link BlogPost} entities.
 * Provides slug-based lookups and filtered queries for published posts.
 */
@Repository
public interface BlogRepository extends JpaRepository<BlogPost, Long> {

    /**
     * Finds an article by its unique, frontend-facing slug.
     *
     * @param slug SEO-friendly article identifier
     * @return matching post when one exists
     */
    Optional<BlogPost> findBySlug(String slug);

    /**
     * Publication-aware lookups used by the public read endpoints.
     *
     * <p>The unfiltered {@code findById}/{@code findBySlug} above must not serve
     * anonymous callers: they return unpublished drafts, so anyone could walk IDs
     * and read embargoed content before the club intends to publish it. Admin
     * paths keep using the unfiltered finders.
     */
    Optional<BlogPost> findByIdAndPublishedTrue(Long id);

    Optional<BlogPost> findBySlugAndPublishedTrue(String slug);

    /**
     * Checks a generated slug before creating an article to avoid unique-constraint errors.
     *
     * @param slug candidate slug
     * @return whether the slug is already assigned
     */
    Boolean existsBySlug(String slug);

    /**
     * Retrieves only posts visible to public readers, preserving pagination metadata.
     *
     * @param pageable requested page and sort order
     * @return page of published posts
     */
    Page<BlogPost> findByPublishedTrue(Pageable pageable);
}
