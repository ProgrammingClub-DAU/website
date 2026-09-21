package com.cpclub.backend.compete.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "compete_matches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {

    @Id
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false)
    private MatchMode mode;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "timeout_minutes")
    private Integer timeoutMinutes;

    @Column(name = "replace_increment")
    private Integer replaceIncrement;

    @Column(name = "grid_size")
    private Integer gridSize;

    @Column(name = "last_polled_at", nullable = false)
    private LocalDateTime lastPolledAt;

    @Column(name = "min_rating")
    private Integer minRating;

    @Column(name = "max_rating")
    private Integer maxRating;

    @Column(name = "show_ratings", nullable = false)
    private Boolean showRatings;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Problem> problems;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Team> teams;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolveLog> solveLogs;
}
