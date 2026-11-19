package com.cpclub.backend.codeforces.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A Codeforces problem a member submitted to without an accepted verdict: the
 * latest failed submission (V31). Written in bulk by the submission sync; read
 * by practice for "try again" suggestions, which leave out problems since solved.
 */
@Entity
@Table(name = "cf_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CfAttempt {

    @EmbeddedId
    private CfAttemptId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("problemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private CfProblem problem;

    /** UTC (D31). */
    @Column(name = "last_attempt_at", nullable = false)
    private LocalDateTime lastAttemptAt;

    @Column(name = "last_submission_id", nullable = false)
    private Long lastSubmissionId;

    @Column(name = "last_verdict", nullable = false, length = 40)
    private String lastVerdict;
}
