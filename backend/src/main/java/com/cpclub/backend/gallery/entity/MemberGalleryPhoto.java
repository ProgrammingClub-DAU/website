package com.cpclub.backend.gallery.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A batch-wise group photo of club members.
 *
 * <p>Distinct from both {@code EventPhoto}, which belongs to an event, and the
 * individual avatar URL on {@code User}: these are group photographs of an
 * admission batch, filtered publicly by {@link #batchYear}.</p>
 */
@Entity
@Table(name = "member_gallery", indexes = {
        @Index(name = "idx_member_gallery_batch_year", columnList = "batch_year")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberGalleryPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Admission batch this photo belongs to, e.g. 2023. */
    @Column(name = "batch_year", nullable = false)
    private Integer batchYear;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    private String caption;

    /** The admin who uploaded this photo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false, nullable = false)
    private LocalDateTime uploadedAt;
}
