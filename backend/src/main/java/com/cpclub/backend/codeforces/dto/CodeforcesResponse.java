package com.cpclub.backend.codeforces.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Immutable representation of the Codeforces {@code user.info} API envelope.
 * Unknown provider fields are ignored so harmless API additions do not break syncs.
 *
 * @param status provider status, normally {@code OK}
 * @param comment provider error explanation when the request fails
 * @param result rating records returned for the requested handles
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CodeforcesResponse(
        String status,
        String comment,
        List<CodeforcesUserDto> result
) {
}
