package com.cpclub.backend.auth.dto;

/**
 * Small immutable response used by endpoints that need to return only a message.
 *
 * @param message human-readable result suitable for client feedback
 */
public record MessageResponse(
        String message
) {
}
