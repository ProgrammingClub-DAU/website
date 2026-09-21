package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.OrderBy;
import jakarta.persistence.CascadeType;
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
     * What kind of event this is, for the badge on the public timeline.
     *
     * <p>Nullable: every event created before this existed has none, and a badge
     * reading OTHER across the club's whole history would be worse than no badge
     * (V11).</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 40)
    private EventType eventType;

    /**
     * The Codeforces contest this event ran on, if it ran on one.
     *
     * <p>Its presence is what makes an event a contest as far as the public
     * page is concerned: an event with no link shows title, date, place,
     * description and photos, and nothing about results. A workshop is not an
     * event with an empty podium, it is an event with no podium.</p>
     */
    @Column(name = "codeforces_contest_url", length = 512)
    private String codeforcesContestUrl;

    /**
     * What the public may see of the results, decided per item.
     *
     * <p>Three switches rather than one, because the club announces these at
     * different moments: the contest link often goes out as the round opens,
     * the winners cannot be known until it closes, and the turnout figure is
     * sometimes not published at all.</p>
     *
     * <p>All default to false, so a new event reveals nothing until somebody
     * decides it should. Admins always see the real values -- these gate the
     * public projection, not the record.</p>
     */
    @Builder.Default
    @Column(name = "show_contest_link", nullable = false)
    private boolean showContestLink = false;

    @Builder.Default
    @Column(name = "show_winners", nullable = false)
    private boolean showWinners = false;

    @Builder.Default
    @Column(name = "show_attendee_count", nullable = false)
    private boolean showAttendeeCount = false;

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

    /**
     * The podium, at most three rows.
     *
     * <p>Cascaded and orphan-removing, unlike photos: setting an event's
     * winners is a single replace-the-podium operation, so the old rows should
     * go when they leave this list rather than being deleted by hand.</p>
     */
    @OneToMany(mappedBy = "event", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<EventWinner> winners = new ArrayList<>();

    @Column(name = "hide_podium", nullable = false)
    @Builder.Default
    private boolean hidePodium = false;

    // â”€â”€ Phase 3: RSVPs â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Optional cap on event RSVPs. Null = unlimited capacity. */
    @Column
    private Integer capacity;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
