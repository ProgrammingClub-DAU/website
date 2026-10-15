package com.cpclub.backend.mail.repository;

import com.cpclub.backend.mail.entity.EmailVerificationToken;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    List<EmailVerificationToken> findByUserAndUsedAtIsNull(User user);

    @Modifying
    @Query("UPDATE EmailVerificationToken t SET t.usedAt = :now WHERE t.user = :user AND t.usedAt IS NULL")
    void markUnusedAsUsedForUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
