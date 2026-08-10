package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CodeforcesResponse(
        String status,
        String comment,
        List<CodeforcesUserDto> result
) {
}
