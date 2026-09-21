package com.cpclub.backend.score.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PracticeSuggestionSkipId implements Serializable {
    private Long userId;
    private Long problemId;
}
