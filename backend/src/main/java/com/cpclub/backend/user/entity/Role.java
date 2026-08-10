package com.cpclub.backend.user.entity;

/**
 * Enumeration of user roles within the CP Club platform.
 * Defines permissions for regular users vs administrators.
 */
public enum Role {
    /**
     * Standard club member. Can view directory, leaderboard, and profile.
     */
    ROLE_USER,

    /**
     * System Administrator. Can manage blog posts and perform role changes.
     */
    ROLE_ADMIN
}

