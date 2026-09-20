package com.cpclub.backend.codeforces.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * One distinct accepted solve by a member on Codeforces.
 * first_ac_at records the earliest accepted submission, stored in UTC (D31).
 * participant_type distinguishes contest solves from practice (D3).
 */
@Entity
@Table(name = "cf_solves")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CfSolve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id", nullable = false)
    private CfProblem problem;

    /** UTC timestamp of the first accepted submission (D2, D31). */
    @Column(name = "first_ac_at", nullable = false)
    private LocalDateTime firstAcAt;

    @Column(name = "first_ac_submission_id", nullable = false)
    private Long firstAcSubmissionId;

    /**
     * Codeforces participantType: CONTESTANT, PRACTICE, VIRTUAL,
     * OUT_OF_COMPETITION, or MANAGER.
     */
    @Column(name = "participant_type", nullable = false, length = 30)
    private String participantType;
}