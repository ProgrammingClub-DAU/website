package com.cpclub.backend.compete.repository;

import com.cpclub.backend.compete.entity.Problem;
import com.cpclub.backend.compete.entity.ProblemId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, ProblemId> {
}
