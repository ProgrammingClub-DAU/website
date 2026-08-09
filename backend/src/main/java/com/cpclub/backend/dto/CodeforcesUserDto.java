package com.cpclub.backend.dto;

import lombok.Data;

@Data
public class CodeforcesUserDto {
    private String handle;
    private Integer rating;
    private Integer maxRating;
    private String rank;
}
