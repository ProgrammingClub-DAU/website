package com.cpclub.backend.contest.repository;

import com.cpclub.backend.contest.entity.ClubContestUpsolve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClubContestUpsolveRepository extends JpaRepository<ClubContestUpsolve, ClubContestUpsolve.Key> {

    @Modifying
    @Query("DELETE FROM ClubContestUpsolve u WHERE u.id.clubContestId = :contestId")
    int deleteByContestId(@Param("contestId") Long contestId);

    /** (user id, problem index) for each recorded upsolve by these members in this contest. */
    @Query("SELECT u.id.userId, u.id.problemIndex FROM ClubContestUpsolve u "
            + "WHERE u.id.clubContestId = :contestId AND u.id.userId IN :userIds")
    List<Object[]> findForMembers(@Param("contestId") Long contestId, @Param("userIds") Collection<Long> userIds);
}
