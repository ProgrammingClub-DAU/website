package com.cpclub.backend.blog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/**
 * Database entity representing a club blog post or editorial article.
 * Indexes on slug (for SEO lookups) and published flag (for filtered queries).
 */
@Entity
@Table(name = "blog_posts", indexes = {
        @Index(name = "idx_blog_slug", columnList = "slug"),
        @Index(name = "idx_blog_published", columnList = "published")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlogPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    private String tags;

    @Builder.Default
    private Boolean published = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // â”€â”€ Phase 3 workflow â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BlogStatus status = BlogStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    /**
     * Keeps the legacy boolean in step with the new status enum during rollout.
     * The legacy boolean is dropped in a Phase 4 cleanup.
     */
    @PrePersist
    @PreUpdate
    public void syncPublishedFlag() {
        this.published = (this.status == BlogStatus.PUBLISHED);
        if (this.published && this.publishedAt == null) {
            this.publishedAt = LocalDateTime.now();
        }
    }
}

