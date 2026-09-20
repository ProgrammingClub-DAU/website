package com.cpclub.backend.contest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A named season grouping club contests for the Championship (Section 1.3).
 * count_best = null means every contest in the season counts.
 */
@Entity
@Table(name = "club_seasons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubSeason {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    @Column(name = "ends_on", nullable = false)
    private LocalDate endsOn;

    /** Null = every contest counts (Section 1.3). */
    @Column(name = "count_best")
    private Integer countBest;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
