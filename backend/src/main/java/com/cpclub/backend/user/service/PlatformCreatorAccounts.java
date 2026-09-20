package com.cpclub.backend.user.service;

import java.util.Set;

/** The six DAU accounts that built and maintain this website. */
public final class PlatformCreatorAccounts {

    private static final Set<String> EMAILS = Set.of(
            "202401226@dau.ac.in",
            "202401474@dau.ac.in",
            "202401152@dau.ac.in",
            "202401041@dau.ac.in",
            "202401178@dau.ac.in",
            "202403019@dau.ac.in"
    );

    private PlatformCreatorAccounts() { }

    public static boolean contains(String email) {
        return email != null && EMAILS.contains(email.trim().toLowerCase());
    }
}
