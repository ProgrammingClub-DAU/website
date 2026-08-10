package com.cpclub.backend.codeforces;

import lombok.Data;
import java.util.List;

public record CodeforcesResponse(
    String status,
    List<CodeforcesUserDto> result,
    String comment
) {}
