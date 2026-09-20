package com.cpclub.backend.codeforces.repository;

import com.cpclub.backend.codeforces.entity.CfSolve;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CfSolveRepository extends JpaRepository<CfSolve, Long> {
    void deleteByUser(User user);
    Optional<CfSolve> findByUserAndProblemId(User user, Long problemId);
}
