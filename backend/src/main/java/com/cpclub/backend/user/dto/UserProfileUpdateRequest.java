package com.cpclub.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Validated editable subset of a member profile.
 *
 * <p>Every field except {@code name} is optional. The column lengths mirror
 * {@code V2__extend_user_profile.sql} exactly, because the production profile runs
 * {@code ddl-auto: validate} and a mismatch fails startup.</p>
 *
 * <p>{@code phoneNumber} is optional here even though the profile form marks it
 * required. Members created before Phase 2 have no phone number, and rejecting
 * their next profile save would lock them out of editing anything at all until
 * they filled in a field they never agreed to supply.</p>
 *
 * @param name replacement display name
 * @param phoneNumber contact number; the frontend asks for it, the API does not insist
 * @param codeforcesHandle optional Codeforces handle to associate with the profile
 * @param leetcodeHandle optional LeetCode username, synced for contest rating
 * @param codechefUrl optional CodeChef profile link; no public API exists to sync it
 * @param atcoderUrl optional AtCoder profile link; likewise unsyncable
 * @param githubUrl optional GitHub profile link
 * @param linkedinUrl optional LinkedIn profile link
 * @param avatarUrl Cloudinary URL of the member's photo, uploaded browser-side
 */
public record UserProfileUpdateRequest(
        @NotBlank(message = "Name must not be blank")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @Size(max = 20, message = "Phone number must be at most 20 characters")
        String phoneNumber,

        @Size(max = 100, message = "Codeforces handle must be at most 100 characters")
        String codeforcesHandle,

        @Size(max = 100, message = "LeetCode handle must be at most 100 characters")
        String leetcodeHandle,

        @Size(max = 512, message = "CodeChef URL must be at most 512 characters")
        String codechefUrl,

        @Size(max = 512, message = "AtCoder URL must be at most 512 characters")
        String atcoderUrl,

        @Size(max = 512, message = "GitHub URL must be at most 512 characters")
        String githubUrl,

        @Size(max = 512, message = "LinkedIn URL must be at most 512 characters")
        String linkedinUrl,

        @Size(max = 512, message = "Avatar URL must be at most 512 characters")
        String avatarUrl
) {
}
