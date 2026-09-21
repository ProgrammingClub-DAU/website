package com.cpclub.backend.compete.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemId implements Serializable {

    @Column(name = "contest_id", nullable = false)
    private Integer contestId;

    @Column(name = "index", nullable = false)
    private String index;

    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProblemId that = (ProblemId) o;
        return Objects.equals(contestId, that.contestId) &&
               Objects.equals(index, that.index) &&
               Objects.equals(matchId, that.matchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contestId, index, matchId);
    }
}
