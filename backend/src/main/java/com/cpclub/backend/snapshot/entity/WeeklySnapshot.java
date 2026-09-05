package com.cpclub.backend.snapshot.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A member's rating on one platform at one point in time, written weekly.
 *
 * <p>Rows are appended by the Monday cron from the rating values already stored
 * on the users table; no external API is called at snapshot time. They are the
 * source for the rating history charts on the profile page.</p>
 *
 * <p>{@link #platform} is a plain string rather than a Java enum. The valid set
 * is closed by the weekly_snapshots_platform_check constraint in
 * V7__create_weekly_snapshots.sql; see {@link #PLATFORM_CODEFORCES} and
 * {@link #PLATFORM_LEETCODE} for the accepted values.</p>
 */
@Entity
@Table(name = "weekly_snapshots", indexes = {
        @Index(name = "idx_weekly_snapshots_user_platform",
                columnList = "user_id, platform, recorded_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySnapshot {

    /** Accepted value of {@link #platform} for Codeforces ratings. */
    public static final String PLATFORM_CODEFORCES = "CODEFORCES";

    /** Accepted value of {@link #platform} for LeetCode ratings. */
    public static final String PLATFORM_LEETCODE = "LEETCODE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String platform;

    @Column(nullable = false)
    private Integer rating;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false, nullable = false)
    private LocalDateTime recordedAt;
}
