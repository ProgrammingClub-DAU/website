package com.cpclub.backend.mail.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Idempotency log for outbound reminder emails (D29).
 * The unique constraint on (user_id, kind, ref_key) ensures a re-run cron
 * never sends the same email twice.
 */
@Entity
@Table(name = "reminder_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReminderKind kind;

    /**
     * Identifies the specific reminder: "event:42" for an event reminder,
     * or "digest:2026-11-03" for a weekly contest digest.
     */
    @Column(name = "ref_key", nullable = false, length = 120)
    private String refKey;

    @CreationTimestamp
    @Column(name = "sent_at", updatable = false)
    private LocalDateTime sentAt;
}
