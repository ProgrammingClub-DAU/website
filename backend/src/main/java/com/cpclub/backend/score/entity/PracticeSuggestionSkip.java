package com.cpclub.backend.score.entity;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A member permanently hiding one practice suggestion (Section 1.4). */
@Entity
@Table(name = "practice_suggestion_skips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSuggestionSkip {

    @EmbeddedId
    private PracticeSuggestionSkipId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("problemId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "problem_id")
    private CfProblem problem;

    /** UTC timestamp (D31). */
    @Column(name = "skipped_at", nullable = false)
    private LocalDateTime skippedAt;
}
