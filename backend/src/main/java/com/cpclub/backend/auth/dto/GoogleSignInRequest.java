package com.cpclub.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * The credential Google's sign-in button hands the browser.
 *
 * <p>This is the whole of what the client sends. Nothing else it could claim about
 * itself would be worth reading: the email address, the display name and the
 * domain are all taken from inside the signed token, never from the request body,
 * so a caller cannot assert who they are.</p>
 *
 * @param idToken the signed ID token from Google, verified before it is trusted
 */
public record GoogleSignInRequest(

        @NotBlank(message = "Sign-in token is required")
        String idToken
) {
}
