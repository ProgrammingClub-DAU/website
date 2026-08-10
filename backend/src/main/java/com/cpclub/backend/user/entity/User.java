package com.cpclub.backend.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Database entity mapping representing a registered member of the CP Club.
 * Indexes are configured on Codeforces handle and rating to optimize leaderboard queries.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_codeforces_handle", columnList = "codeforces_handle"),
        @Index(name = "idx_rating", columnList = "rating")
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
