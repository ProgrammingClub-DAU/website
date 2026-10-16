package com.cpclub.backend.blog.repository;

import com.cpclub.backend.blog.entity.BlogComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface BlogCommentRepository extends JpaRepository<BlogComment, Long> {

    @Query("SELECT c FROM BlogComment c JOIN FETCH c.user LEFT JOIN FETCH c.parent "
            + "WHERE c.post.id = :postId ORDER BY c.createdAt ASC, c.id ASC")
    List<BlogComment> findThread(@Param("postId") Long postId);

    /** Visible comments per post, for the list view's counts in one query. */
    @Query("SELECT c.post.id, COUNT(c) FROM BlogComment c "
            + "WHERE c.post.id IN :postIds AND c.deletedAt IS NULL GROUP BY c.post.id")
    List<Object[]> countVisibleByPostIds(@Param("postIds") Collection<Long> postIds);

    long countByPostIdAndDeletedAtIsNull(Long postId);

    /**
     * Removes a post's comments before the post itself. Replies first, then the
     * comments they hang from: the H2 test schema has no ON DELETE CASCADE, so the
     * order matters there.
     */
    @Modifying
    @Query("DELETE FROM BlogComment c WHERE c.post.id = :postId AND c.parent IS NOT NULL")
    int deleteRepliesByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("DELETE FROM BlogComment c WHERE c.post.id = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);
}
