package com.cpclub.backend.auth.dto;

import com.cpclub.backend.user.entity.Role;

/**
 * Immutable authentication result returned after a successful registration or login.
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
 */
public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        Role role,
        String codeforcesHandle
) {
}
