package com.cpclub.backend.contest.repository;

import com.cpclub.backend.contest.entity.ClubSeason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubSeasonRepository extends JpaRepository<ClubSeason, Long> {

    List<ClubSeason> findAllByOrderByStartsOnDesc();
}
