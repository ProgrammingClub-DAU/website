package com.cpclub.backend.compete.repository;

import com.cpclub.backend.compete.entity.SolveLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolveLogRepository extends JpaRepository<SolveLog, Long> {
}
