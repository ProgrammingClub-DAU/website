package com.cpclub.backend.compete.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "compete_solve_logs",
    uniqueConstraints = @UniqueConstraint(name = "uq_solve_log_match_problem",
        columnNames = {"match_id", "contest_id", "index"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolveLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "handle", nullable = false)
    private String handle;

    @Column(name = "team", nullable = false)
    private String team;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "score")
    private Integer score;

    @Column(name = "contest_id", nullable = false)
    private Integer contestId;

    @Column(name = "index", nullable = false)
    private String index;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "contest_id", referencedColumnName = "contest_id", nullable = false, insertable = false, updatable = false),
        @JoinColumn(name = "index", referencedColumnName = "index", nullable = false, insertable = false, updatable = false),
        @JoinColumn(name = "match_id", referencedColumnName = "match_id", nullable = false, insertable = false, updatable = false)
    })
    private Problem problem;
}
