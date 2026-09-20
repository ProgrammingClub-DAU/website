package com.cpclub.backend.codeforces.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cf_problem_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CfProblemTag {

    @EmbeddedId
    private CfProblemTagId id;

    @MapsId("problemId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id")
    private CfProblem problem;

    @Column(name = "tag", insertable = false, updatable = false)
    private String tag;
}