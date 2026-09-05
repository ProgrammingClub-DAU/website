package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Join entity recording that a member attended an event.
 *
 * <p>Attendance is an official club record: only an admin can create these rows,
 * and {@link #addedBy} keeps each one attributable. The (event_id, user_id) pair
 * is unique at the database level, so two admins adding the same student
 * concurrently cannot both succeed.</p>
 */
@Entity
@Table(
        name = "event_attendees",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_event_attendees_event_user",
                columnNames = {"event_id", "user_id"}
        ),
        indexes = {
                @Index(name = "idx_event_attendees_event", columnList = "event_id"),
                @Index(name = "idx_event_attendees_user", columnList = "user_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    /** The student who attended. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The admin who recorded this attendance. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "added_by", nullable = false)
    private User addedBy;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false, nullable = false)
    private LocalDateTime addedAt;
}
