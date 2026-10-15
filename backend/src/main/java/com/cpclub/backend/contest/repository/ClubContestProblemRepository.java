package com.cpclub.backend.contest.repository;

import com.cpclub.backend.contest.entity.ClubContestProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ClubContestProblemRepository extends JpaRepository<ClubContestProblem, Long> {

    List<ClubContestProblem> findByClubContestIdOrderByProblemIndexAsc(Long clubContestId);

    List<ClubContestProblem> findByClubContestIdInOrderByProblemIndexAsc(Collection<Long> clubContestIds);
}
