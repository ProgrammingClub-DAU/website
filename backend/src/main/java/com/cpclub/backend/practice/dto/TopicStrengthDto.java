package com.cpclub.backend.practice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicStrengthDto {
    private String tag;
    private int memberSolves;
    private double memberShare;
    private int clubSolves;
    private double clubShare;
    private double gap;
    /** The member's estimated level in this topic; null for the "other" bucket. */
    private Integer level;
    /** One of the member's focus topics. */
    private boolean weak;
}
