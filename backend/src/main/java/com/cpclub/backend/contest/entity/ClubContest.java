package com.cpclub.backend.contest.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A Codeforces contest registered as a club event (gym, mashup, group, or official).
 * starts_at and last_synced_at are stored UTC (D31).
 */
@Entity
@Table(name = "club_contests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubContest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cf_contest_id", nullable = false, unique = true)
    private Integer cfContestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id")
    private ClubSeason season;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClubContestKind kind;

    @Column(nullable = false)
    private String name;

    /** UTC start time (D31). */
    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    /** UTC timestamp of the most recent standings sync (D31). */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
