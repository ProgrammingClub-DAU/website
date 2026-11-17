package com.cpclub.backend.contest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * A problem a member solved in a club contest's own standings after it ended: a
 * PRACTICE or VIRTUAL row on Codeforces (V29). Rebuilt on every standings sync.
 */
@Entity
@Table(name = "club_contest_upsolves")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClubContestUpsolve {

    @EmbeddedId
    private Key id;

    public ClubContestUpsolve(Long clubContestId, Long userId, String problemIndex) {
        this(new Key(clubContestId, userId, problemIndex));
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Key implements Serializable {

        @Column(name = "club_contest_id", nullable = false)
        private Long clubContestId;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Column(name = "problem_index", nullable = false, length = 10)
        private String problemIndex;
    }
}
