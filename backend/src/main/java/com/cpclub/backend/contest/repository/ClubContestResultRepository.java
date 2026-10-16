package com.cpclub.backend.contest.repository;

import com.cpclub.backend.contest.entity.ClubContestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClubContestResultRepository extends JpaRepository<ClubContestResult, Long> {

    @Query("SELECT r FROM ClubContestResult r JOIN FETCH r.user "
            + "WHERE r.clubContest.id = :contestId ORDER BY r.contestRank ASC, r.user.name ASC")
    List<ClubContestResult> findRanklist(@Param("contestId") Long contestId);

    @Query("SELECT r FROM ClubContestResult r JOIN FETCH r.user JOIN FETCH r.clubContest "
            + "WHERE r.clubContest.id IN :contestIds")
    List<ClubContestResult> findByContestIds(@Param("contestIds") Collection<Long> contestIds);

    @Query("SELECT r FROM ClubContestResult r JOIN FETCH r.clubContest WHERE r.user.id = :userId")
    List<ClubContestResult> findByUserId(@Param("userId") Long userId);

    @Query("SELECT r.clubContest.id, COUNT(r) FROM ClubContestResult r GROUP BY r.clubContest.id")
    List<Object[]> countPerContest();

    /**
     * Bulk delete, so a resync replaces the previous rows in one statement. The
     * per-problem rows go with them: they are deleted first here rather than relying
     * on {@code ON DELETE CASCADE}, which the H2 test schema does not have.
     */
    @Modifying
    @Query("DELETE FROM ClubContestResult r WHERE r.clubContest.id = :contestId")
    int deleteByContestId(@Param("contestId") Long contestId);
}
