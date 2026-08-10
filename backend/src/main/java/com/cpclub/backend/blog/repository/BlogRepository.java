package com.cpclub.backend.blog.repository;

import com.cpclub.backend.blog.entity.BlogPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlogRepository extends JpaRepository<BlogPost, Long> {

    Optional<BlogPost> findBySlug(String slug);

    Boolean existsBySlug(String slug);

    Page<BlogPost> findByPublishedTrue(Pageable pageable);
}
