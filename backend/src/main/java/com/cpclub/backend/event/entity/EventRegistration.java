package com.cpclub.backend.event.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A member's RSVP for an upcoming event (D13).
 * Distinct from admin-marked attendance â€” RSVP is the member's own intent.
 * registered_at is UTC (D31).
 */
@Entity
@Table(name = "event_registrations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;
}
