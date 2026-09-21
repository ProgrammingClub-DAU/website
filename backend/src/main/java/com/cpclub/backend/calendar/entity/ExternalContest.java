package com.cpclub.backend.calendar.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A contest from an external platform's public schedule (D26).
 * Synced every 6 hours. Used to populate the upcoming-contests calendar.
 * starts_at and synced_at are stored UTC (D31).
 */
@Entity
@Table(name = "external_contests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalContest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContestPlatform platform;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 512)
    private String url;

    /** UTC start time (D31). */
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    /** UTC timestamp of the most recent sync (D31). */
    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;
}
