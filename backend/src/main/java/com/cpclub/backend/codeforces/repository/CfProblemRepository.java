package com.cpclub.backend.codeforces.repository;

import com.cpclub.backend.codeforces.entity.CfProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CfProblemRepository extends JpaRepository<CfProblem, Long> {
    Optional<CfProblem> findByProblemKey(String problemKey);
    
    @Query(value = "SELECT cp.* FROM cf_problems cp WHERE cp.rating >= :minRating AND cp.rating <= :maxRating AND NOT EXISTS (SELECT 1 FROM cf_problem_tags t WHERE t.problem_id = cp.id AND t.tag_name = '*special') ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<CfProblem> findRandomProblemsByRatingRange(@Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating, @Param("limit") Integer limit);
}
