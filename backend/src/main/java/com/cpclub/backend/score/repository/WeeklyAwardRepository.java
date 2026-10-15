package com.cpclub.backend.score.repository;

import com.cpclub.backend.score.entity.AwardType;
import com.cpclub.backend.score.entity.WeeklyAward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeeklyAwardRepository extends JpaRepository<WeeklyAward, Long> {

    List<WeeklyAward> findByWeekStartOrderByAwardTypeAscUserIdAsc(LocalDate weekStart);

    List<WeeklyAward> findByUserIdOrderByWeekStartDescAwardTypeAsc(Long userId);

    boolean existsByWeekStartAndAwardType(LocalDate weekStart, AwardType awardType);

    boolean existsByWeekStart(LocalDate weekStart);

    @Query("SELECT MAX(w.weekStart) FROM WeeklyAward w")
    LocalDate findLatestWeekStart();

    @Modifying
    @Query("DELETE FROM WeeklyAward w WHERE w.weekStart = :weekStart")
    void deleteByWeekStart(@Param("weekStart") LocalDate weekStart);
}
