package com.cpclub.backend.blog.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A comment on a published blog post (D19).
 * One level of replies (parent_id). Deletion is soft (deleted_at).
 * Authors may edit within 15 minutes; admins may delete at any time.
 */
@Entity
@Table(name = "blog_comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private BlogPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Non-null for a reply; null for a top-level comment. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private BlogComment parent;

    @Column(nullable = false, length = 2000)
    private String body;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Set when the author edits within the 15-minute window. UTC (D31). */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Soft delete timestamp. Non-null = deleted; rendered as "[deleted]". UTC (D31). */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
