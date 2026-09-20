package com.cpclub.backend.mail.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A single-use email verification token (V17).
 * The raw token is emailed; only its SHA-256 hex is stored here.
 * Kept for Stage 1C (Spring Mail) even though the primary sign-in flow
 * (Google-only, A2) does not use it.
 */
@Entity
@Table(name = "email_verification_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hex of the raw token. The raw token only ever exists in the email. */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    /** UTC expiry (D31). */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** UTC timestamp when the token was consumed (D31). Null = not yet used. */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
