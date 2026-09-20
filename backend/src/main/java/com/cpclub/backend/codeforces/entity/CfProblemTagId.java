package com.cpclub.backend.codeforces.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CfProblemTagId implements Serializable {
    private Long problemId;
    private String tag;
}