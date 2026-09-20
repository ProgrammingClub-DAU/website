package com.cpclub.backend.codeforces.repository;

import com.cpclub.backend.codeforces.entity.CfProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CfProblemRepository extends JpaRepository<CfProblem, Long> {
    Optional<CfProblem> findByProblemKey(String problemKey);
    
    @Query(value = "SELECT * FROM cf_problems WHERE rating >= :minRating AND rating <= :maxRating ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<CfProblem> findRandomProblemsByRatingRange(@Param("minRating") Integer minRating, @Param("maxRating") Integer maxRating, @Param("limit") Integer limit);
}
