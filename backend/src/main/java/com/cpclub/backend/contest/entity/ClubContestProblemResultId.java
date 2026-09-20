package com.cpclub.backend.contest.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestProblemResultId implements Serializable {
    private Long resultId;
    private Long problemId;
}
