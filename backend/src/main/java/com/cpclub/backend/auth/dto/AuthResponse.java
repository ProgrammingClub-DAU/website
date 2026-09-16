package com.cpclub.backend.auth.dto;

import com.cpclub.backend.user.entity.AcademicYear;
import com.cpclub.backend.user.entity.Role;

/**
 * Immutable authentication result returned after a successful sign-in.
 *
 * <p>It intentionally contains only the signed access token and client-safe identity
 * attributes; password hashes and other internal security state never leave the API.</p>
 *
 * @param token signed JWT used for subsequent authenticated requests
 * @param id persisted user identifier
 * @param name member display name
 * @param email normalized sign-in email
 * @param role authorization role embedded in the token
 * @param codeforcesHandle optional linked Codeforces handle
 * @param academicYear the member's year, or null if they have never been asked;
 *                     null is how the client knows to send a first-time member to
 *                     the welcome step instead of straight to the site
 */
public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        Role role,
        String codeforcesHandle,
        AcademicYear academicYear
) {
}
