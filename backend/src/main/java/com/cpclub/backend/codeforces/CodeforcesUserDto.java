package com.cpclub.backend.codeforces;

import lombok.Data;

public record CodeforcesUserDto(
    String handle,
    Integer rating,
    Integer maxRating,
    String rank
) {}
