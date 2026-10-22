package com.cpclub.backend.calendar.repository;

import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Purges contests that ended long ago.
     *
     * <p>An explicit bulk delete with its own write transaction. It was a derived
     * {@code deleteBy...} called from outside any transaction, which Spring Data
     * runs inside the repository's read-only default -- so the removals were never
     * flushed and old contests piled up for ever.</p>
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ExternalContest c WHERE c.startsAt < :cutoff")
    int deleteByStartsAtLessThan(@Param("cutoff") LocalDateTime cutoff);
}
