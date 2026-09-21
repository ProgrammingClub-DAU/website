package com.cpclub.backend.stats.repository;

import com.cpclub.backend.stats.entity.PlatformDailyTotal;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PlatformDailyTotalRepository extends JpaRepository<PlatformDailyTotal, Long> {
    Optional<PlatformDailyTotal> findByUserAndPlatformAndCapturedOn(User user, String platform, LocalDate capturedOn);
}
