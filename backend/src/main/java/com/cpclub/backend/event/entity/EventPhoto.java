package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * One photo in the gallery of an event.
 *
 * <p>Stores the Cloudinary secure URL only. Uploads go from the browser to
 * Cloudinary via the Upload Widget and the backend records the returned string,
 * which keeps image binaries out of PostgreSQL entirely.</p>
 */
@Entity
@Table(name = "event_photos", indexes = {
        @Index(name = "idx_event_photos_event", columnList = "event_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

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
