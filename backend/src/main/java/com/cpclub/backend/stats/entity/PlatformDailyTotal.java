package com.cpclub.backend.stats.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Daily LeetCode solved-total baseline for a member.
 * captured_on is the IST calendar date (D1, D6); the unique constraint
 * prevents duplicate baselines for the same date.
 */
@Entity
@Table(name = "platform_daily_totals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformDailyTotal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Currently only LEETCODE -- the check constraint enforces this. */
    @Column(nullable = false, length = 20)
    private String platform;

    /** IST calendar date this baseline belongs to. */
    @Column(name = "captured_on", nullable = false)
    private LocalDate capturedOn;

    @Column(name = "total_solved", nullable = false)
    private int totalSolved;

    @Column(name = "easy_solved")
    private Integer easySolved;

    @Column(name = "medium_solved")
    private Integer mediumSolved;

    @Column(name = "hard_solved")
    private Integer hardSolved;
}