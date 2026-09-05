package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Database entity representing a club event -- a workshop, contest, or session.
 * Indexed on status and date, the two columns the public listings filter and
 * order by.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_events_status", columnList = "status"),
        @Index(name = "idx_events_event_date", columnList = "event_date")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.UPCOMING;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    /**
     * The admin who created this event. Lazy: event listings render title, date,
     * and location only, so eager-loading the creator would add one join per row
     * for a field the public pages never show.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Photos attached to this event, for the gallery on the detail page.
     *
     * <p>Read-only convenience association: photos are created and deleted
     * through EventPhoto directly, so there is no cascade here. Deleting an
     * event cascades to its photos at the database level instead
     * (ON DELETE CASCADE in V5), which is moot in practice because events are
     * cancelled rather than deleted.</p>
     */
    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY)
    @Builder.Default
    private List<EventPhoto> photos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
