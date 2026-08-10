package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CodeforcesUserDto(
        String handle,
        Integer rating,
        Integer maxRating,
        String rank,
        String maxRank
) {
}
