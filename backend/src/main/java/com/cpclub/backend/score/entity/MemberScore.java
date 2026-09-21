package com.cpclub.backend.score.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Overall Aggregated Score for one member (D8, Section 1.2).
 * One row per member; upserted daily at 01:00 IST by Stage 3B.
 */
@Entity
@Table(name = "member_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberScore {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "rating_score", nullable = false)
    private int ratingScore;

    @Column(name = "solved_score", nullable = false)
    private int solvedScore;

    @Column(name = "contest_score", nullable = false)
    private int contestScore;

    /** UTC timestamp of the last recompute (D31). */
    @Column(name = "computed_at", nullable = false)
    private LocalDateTime computedAt;
}
