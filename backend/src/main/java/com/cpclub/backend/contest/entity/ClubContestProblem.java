package com.cpclub.backend.contest.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "club_contest_problems")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_contest_id", nullable = false)
    private ClubContest clubContest;

    @Column(name = "problem_index", nullable = false, length = 10)
    private String problemIndex;

    @Column(nullable = false)
    private String name;

    private Integer rating;
}
