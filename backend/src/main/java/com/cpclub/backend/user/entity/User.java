package com.cpclub.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Database entity mapping representing a registered member of the CP Club.
 * Indexes are configured on Codeforces handle and rating to optimize leaderboard queries,
 * on LeetCode handle for the sync lookup, and on club role for the leaderboard filter.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_codeforces_handle", columnList = "codeforces_handle"),
        @Index(name = "idx_rating", columnList = "rating"),
        @Index(name = "idx_leetcode_handle", columnList = "leetcode_handle"),
        @Index(name = "idx_club_role", columnList = "club_role")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "codeforces_handle", unique = true)
    private String codeforcesHandle;

    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    // ── Phase 2 profile fields ────────────────────────────────────────────────
    // Every field below is nullable. Phase 1 rows already exist in production,
    // so V2 could not add NOT NULL columns to a populated table. Fields the UI
    // treats as mandatory (phoneNumber) are enforced in the service layer.
    //
    // Column lengths must match V2__extend_user_profile.sql exactly: the prod
    // profile runs ddl-auto: validate and a mismatch fails startup.

    /** Cloudinary URL of the member's profile photo. Uploaded browser-side. */
    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    /**
     * Contact number. Nullable here, but required before an admin can add this
     * member to an event -- {@code EventService} rejects the attempt otherwise.
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /** LeetCode username, synced every 6 hours via the public GraphQL API. */
    @Column(name = "leetcode_handle", unique = true)
    private String leetcodeHandle;

    /** Latest synced LeetCode contest rating. Zero means "never contested". */
    @Column(name = "leetcode_rating")
    private Integer leetcodeRating;

    /** Profile link only -- CodeChef exposes no stable public API to sync. */
    @Column(name = "codechef_url", length = 512)
    private String codechefUrl;

    /** Profile link only -- AtCoder exposes no stable public API to sync. */
    @Column(name = "atcoder_url", length = 512)
    private String atcoderUrl;

    @Column(name = "github_url", length = 512)
    private String githubUrl;

    @Column(name = "linkedin_url", length = 512)
    private String linkedinUrl;

    /**
     * The member's position in the club. Distinct from {@link #role}, which
     * controls API access -- see {@link ClubRole}. Null means unassigned.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "club_role", length = 50)
    private ClubRole clubRole;

    /** Admission batch, e.g. 2023. Drives the batch gallery filter. */
    @Column(name = "batch_year")
    private Integer batchYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Convenience constructor used by tests and simple creation flows.
     *
     * <p>The authentication service normally uses the builder so it can initialize the
     * optional Codeforces handle; this constructor still safeguards the default role.</p>
     *
     * @param name member display name
     * @param email unique sign-in email
     * @param password BCrypt password hash
     * @param role authorization role, defaulting to {@link Role#ROLE_USER} when absent
     */
    public User(String name, String email, String password, Role role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role != null ? role : Role.ROLE_USER;
        this.rating = null;
    }
}
