package com.cpclub.backend.compete.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "compete_problems")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @EmbeddedId
    private ProblemId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("matchId")
    @JoinColumn(name = "match_id")
    private Match match;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "max_points")
    private Integer maxPoints;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @OneToMany(mappedBy = "problem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolveLog> solveLogs;
}
