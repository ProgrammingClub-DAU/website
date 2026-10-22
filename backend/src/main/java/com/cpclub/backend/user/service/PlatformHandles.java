package com.cpclub.backend.user.service;

import com.cpclub.backend.common.exception.BadRequestException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns whatever a member typed into a platform handle, or refuses it.
 *
 * <p>Members paste profile links: one handle was stored as
 * {@code https://codeforces.com/profile/Ishaan-K}, which every Codeforces call then
 * rejected, so that member had no rating, solves or contests at all. A link is
 * reduced to the handle in it; a leading {@code @} and surrounding whitespace are
 * dropped; anything still not shaped like a handle is refused with a message the
 * member can act on, instead of being saved and failing every sync silently.</p>
 */
public final class PlatformHandles {

    private static final Pattern CF_URL = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\d?\\.)?codeforces\\.(?:com|ml|es)/(?:profile|submissions)/([^/?#\\s]+).*$",
            Pattern.CASE_INSENSITIVE);
    /** Codeforces: 3-24 characters of Latin letters, digits, underscore, hyphen and dot. */
    private static final Pattern CF_HANDLE = Pattern.compile("^[A-Za-z0-9_.\\-]{3,24}$");

    private static final Pattern LC_URL = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?leetcode\\.(?:com|cn)/(?:u/)?([^/?#\\s]+).*$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern LC_HANDLE = Pattern.compile("^[A-Za-z0-9_.\\-]{1,50}$");
    /** LeetCode paths that look like {@code leetcode.com/<x>} but are not profiles. */
    private static final Set<String> LC_RESERVED = Set.of(
            "problems", "problemset", "contest", "discuss", "explore", "studyplan", "accounts", "u", "profile");

    private PlatformHandles() {
    }

    /** @return the bare Codeforces handle, or null when nothing was supplied */
    public static String normalizeCodeforces(String raw) {
        String value = clean(raw);
        if (value == null) return null;
        Matcher url = CF_URL.matcher(value);
        if (url.matches()) {
            value = url.group(1);
        } else if (value.contains("/") || value.contains(".com")) {
            throw new BadRequestException(
                    "That doesn't look like a Codeforces handle. Enter just the handle, e.g. tourist, "
                            + "or your profile link codeforces.com/profile/<handle>.");
        }
        if (!CF_HANDLE.matcher(value).matches()) {
            throw new BadRequestException("'" + value + "' is not a valid Codeforces handle. Handles are 3-24 "
                    + "characters: letters, digits, underscore, hyphen or dot.");
        }
        return value;
    }

    /** @return the bare LeetCode username, or null when nothing was supplied */
    public static String normalizeLeetcode(String raw) {
        String value = clean(raw);
        if (value == null) return null;
        Matcher url = LC_URL.matcher(value);
        if (url.matches()) {
            value = url.group(1);
            if (LC_RESERVED.contains(value.toLowerCase())) {
                throw new BadRequestException(
                        "That link is not a LeetCode profile. Use leetcode.com/u/<username> or just the username.");
            }
        } else if (value.contains("/") || value.contains(".com")) {
            throw new BadRequestException(
                    "That doesn't look like a LeetCode username. Enter just the username, "
                            + "or your profile link leetcode.com/u/<username>.");
        }
        if (!LC_HANDLE.matcher(value).matches()) {
            throw new BadRequestException("'" + value + "' is not a valid LeetCode username. Usernames use "
                    + "letters, digits, underscore, hyphen or dot.");
        }
        return value;
    }

    private static String clean(String raw) {
        if (raw == null) return null;
        String value = raw.strip();
        while (value.startsWith("@")) value = value.substring(1).strip();
        return value.isEmpty() ? null : value;
    }
}
