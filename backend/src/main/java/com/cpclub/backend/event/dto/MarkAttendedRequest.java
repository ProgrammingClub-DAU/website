package com.cpclub.backend.event.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MarkAttendedRequest(
        @NotEmpty(message = "Choose at least one member")
        @Size(max = 500, message = "At most 500 members at a time")
        List<Long> userIds
) {
}
