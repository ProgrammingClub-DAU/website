package com.cpclub.backend.user.dto;

import com.cpclub.backend.user.entity.ClubRole;
import jakarta.validation.constraints.NotNull;

/**
 * Admin request to change a member's position in the club.
 *
 * <p>Distinct from {@link UpdateRoleRequest}, which changes {@code Role} — the
 * API authorization level. This changes {@link ClubRole}, which is a title and
 * drives the leaderboard filter and the badge beside a member's name. Granting
 * someone the Convenor title does not grant them admin access, and that
 * separation is deliberate.</p>
 *
 * <p>Typed as the enum rather than a String so an unrecognized value is rejected
 * by deserialization with a 400, before it can reach the service.</p>
 *
 * @param clubRole the position to assign
 */
public record UpdateClubRoleRequest(
        @NotNull(message = "Club role must not be null")
        ClubRole clubRole
) {
}
