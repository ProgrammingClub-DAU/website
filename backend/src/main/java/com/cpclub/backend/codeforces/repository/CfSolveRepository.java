package com.cpclub.backend.codeforces.repository;

import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CfSolveRepository extends JpaRepository<CfSolve, Long> {
    void deleteByUser(User user);
    Optional<CfSolve> findByUserAndProblemId(User user, Long problemId);

    /**
     * (user id, problem index) for each member's first accepted solve of a problem in
     * the given Codeforces contest at or after {@code since}. With {@code since} set to
     * a club contest's end, that is the upsolve set (D12): matched on the contest id
     * itself, so solving the original problem in some other contest does not count.
     */
    @Query("SELECT s.user.id, s.problem.problemIndex FROM CfSolve s "
            + "WHERE s.problem.contestId = :cfContestId AND s.firstAcAt >= :since AND s.user.id IN :userIds")
    List<Object[]> findSolvesInContestSince(@Param("cfContestId") Integer cfContestId,
                                           @Param("since") LocalDateTime since,
                                           @Param("userIds") Collection<Long> userIds);
}
