package com.cpclub.backend.contest.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Per-problem outcome for one member in one club contest.
 * solved_in_contest distinguishes a live solve from an upsolve (D12).
 */
@Entity
@Table(name = "club_contest_problem_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestProblemResult {

    @EmbeddedId
    private ClubContestProblemResultId id;

    @MapsId("resultId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id")
    private ClubContestResult result;

    @MapsId("problemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private ClubContestProblem problem;

    /** True = solved during the contest; false = upsolve after it ended (D12). */
    @Column(name = "solved_in_contest", nullable = false)
    private boolean solvedInContest;
}
