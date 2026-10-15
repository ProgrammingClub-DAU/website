package com.cpclub.backend.score.repository;

import com.cpclub.backend.score.entity.MemberScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MemberScoreRepository extends JpaRepository<MemberScore, Long> {

    @Query("SELECT MAX(m.computedAt) FROM MemberScore m")
    Optional<LocalDateTime> findLatestComputedAt();
}
