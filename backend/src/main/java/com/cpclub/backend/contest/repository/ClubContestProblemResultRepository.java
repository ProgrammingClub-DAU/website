package com.cpclub.backend.contest.repository;

import com.cpclub.backend.contest.entity.ClubContestProblemResult;
import com.cpclub.backend.contest.entity.ClubContestProblemResultId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClubContestProblemResultRepository
        extends JpaRepository<ClubContestProblemResult, ClubContestProblemResultId> {

    @Query("SELECT pr FROM ClubContestProblemResult pr JOIN FETCH pr.result r JOIN FETCH pr.problem "
            + "WHERE r.clubContest.id IN :contestIds")
    List<ClubContestProblemResult> findByContestIds(@Param("contestIds") Collection<Long> contestIds);

    @Modifying
    @Query("DELETE FROM ClubContestProblemResult pr WHERE pr.result.id IN "
            + "(SELECT r.id FROM ClubContestResult r WHERE r.clubContest.id = :contestId)")
    int deleteByContestId(@Param("contestId") Long contestId);
}
