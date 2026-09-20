package com.cpclub.backend.codeforces.repository;

import com.cpclub.backend.codeforces.entity.CfProblem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CfProblemRepository extends JpaRepository<CfProblem, Long> {
    Optional<CfProblem> findByProblemKey(String problemKey);
}
