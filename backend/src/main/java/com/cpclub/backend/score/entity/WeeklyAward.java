package com.cpclub.backend.score.entity;

import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One award for one member for one closed week (Section 1.5).
 * Multiple rows per week are valid (ties, or different award types).
 * Once stored, awards are never recomputed automatically (D1, Section 1.5).
 */
@Entity
@Table(name = "weekly_awards")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyAward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The Monday (IST) on which the awarded week began (D1). */
    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Enumerated(EnumType.STRING)
    @Column(name = "award_type", nullable = false, length = 40)
    private AwardType awardType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "metric_value", nullable = false)
    private int metricValue;

    /**
     * Non-null for BIGGEST_RATING_JUMP only: records which platform the jump was on.
     * Stored as the Platform enum name (CODEFORCES or LEETCODE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20)
    private Platform platform;

    /** UTC timestamp of when this award was computed (D31). */
    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;

    /**
     * Null when computed by the scheduler.
     * Set to the admin's user id when recomputed manually (audit trail).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "computed_by")
    private User computedBy;
}
