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
}
