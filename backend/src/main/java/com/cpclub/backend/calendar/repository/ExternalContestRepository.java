package com.cpclub.backend.calendar.repository;

import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface ExternalContestRepository extends JpaRepository<ExternalContest, Long> {
    Optional<ExternalContest> findByPlatformAndExternalId(ContestPlatform platform, String externalId);
    List<ExternalContest> findByPlatform(ContestPlatform platform);
    List<ExternalContest> findByStartsAtGreaterThanEqualAndStartsAtLessThanEqual(LocalDateTime from, LocalDateTime to);
    List<ExternalContest> findByPlatformInAndStartsAtGreaterThanEqualAndStartsAtLessThanEqual(List<ContestPlatform> platforms, LocalDateTime from, LocalDateTime to);
    void deleteByPlatformAndExternalId(ContestPlatform platform, String externalId);
    void deleteByStartsAtLessThan(LocalDateTime date);
}
