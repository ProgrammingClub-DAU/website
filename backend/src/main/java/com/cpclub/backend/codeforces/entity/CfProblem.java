package com.cpclub.backend.codeforces.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * One Codeforces problem from the public problemset.
 * Tags are stored in a join table (D21) so they are indexable and filterable.
 */
@Entity
@Table(name = "cf_problems")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CfProblem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Canonical key: "contestId/index" (e.g. "1843/C"), or
     * "problemsetName/index" for problems without a contest id.
     * Uniqueness constraint is on this column.
     */
    @Column(name = "problem_key", nullable = false, length = 64, unique = true)
    private String problemKey;

    @Column(name = "contest_id")
    private Integer contestId;

    @Column(name = "problemset_name", length = 50)
    private String problemsetName;

    @Column(name = "problem_index", nullable = false, length = 10)
    private String problemIndex;

    @Column(nullable = false)
    private String name;

    /** Codeforces difficulty rating. NULL for unrated problems. */
    private Integer rating;

    /** Community solve count from problemset.problems. Drives practice suggestion ordering. */
    @Column(name = "solved_count")
    private Integer solvedCount;

    @Builder.Default
    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<CfProblemTag> tags = new HashSet<>();
}