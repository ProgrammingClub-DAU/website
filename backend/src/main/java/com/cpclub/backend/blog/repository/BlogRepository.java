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
