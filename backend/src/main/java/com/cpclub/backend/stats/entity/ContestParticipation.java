package com.cpclub.backend.stats.entity;

import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One rated or unrated contest a member entered on Codeforces or LeetCode (D5).
 * Also drives the multi-platform rating chart (O10).
 * started_at is UTC (D31).
 */
@Entity
@Table(name = "contest_participations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(name = "external_contest_id", nullable = false, length = 64)
    private String externalContestId;

    @Column(name = "contest_name", nullable = false)
    private String contestName;

    /** UTC start time of the contest (D31). */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private boolean rated;

    /** Rank among all participants on the platform; null for unrated contests. */
    @Column(name = "contest_rank")
    private Integer contestRank;

    @Column(name = "old_rating")
    private Integer oldRating;

    @Column(name = "new_rating")
    private Integer newRating;
}