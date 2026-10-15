package com.cpclub.backend.contest.repository;

import com.cpclub.backend.contest.entity.ClubContest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClubContestRepository extends JpaRepository<ClubContest, Long> {

    boolean existsByCfContestId(Integer cfContestId);

    @Query("SELECT c FROM ClubContest c LEFT JOIN FETCH c.season ORDER BY c.startsAt DESC")
    List<ClubContest> findAllWithSeasonNewestFirst();

    List<ClubContest> findBySeasonIdOrderByStartsAtAsc(Long seasonId);

    long countBySeasonId(Long seasonId);

    /**
     * Contests the 15-minute sync might need to touch: started, and not more than a
     * few days past their end. The exact rule lives in Java
     * ({@code ClubContestSyncService.needsSync}); this only keeps the candidate list
     * short.
     */
    @Query("SELECT c FROM ClubContest c WHERE c.startsAt <= :now AND c.startsAt >= :earliestStart")
    List<ClubContest> findSyncCandidates(@Param("now") LocalDateTime now,
                                         @Param("earliestStart") LocalDateTime earliestStart);

    /**
     * Detaches a season's contests before the season is deleted.
     *
     * <p>The migration declares {@code ON DELETE SET NULL}, but the H2 test schema is
     * generated from the entities and has no such clause. Clearing it here keeps the
     * behaviour identical on both, rather than depending on the database to do it.</p>
     */
    @Modifying
    @Query("UPDATE ClubContest c SET c.season = null WHERE c.season.id = :seasonId")
    int detachFromSeason(@Param("seasonId") Long seasonId);
}
