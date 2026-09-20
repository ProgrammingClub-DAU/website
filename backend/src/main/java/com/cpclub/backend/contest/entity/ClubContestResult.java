package com.cpclub.backend.contest.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A member's result in a club contest, ranked among club members only.
 * cf_points is the Codeforces standings score, not championship points.
 */
@Entity
@Table(name = "club_contest_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_contest_id", nullable = false)
    private ClubContest clubContest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Rank among club members only â€” not the global Codeforces rank. */
    @Column(name = "contest_rank", nullable = false)
    private int contestRank;

    @Column(name = "cf_points", nullable = false, precision = 10, scale = 2)
    private BigDecimal cfPoints;

    @Column(nullable = false)
    private int penalty;

    @Column(name = "solved_count", nullable = false)
    private int solvedCount;
}
