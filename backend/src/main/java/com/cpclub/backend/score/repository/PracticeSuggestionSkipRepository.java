package com.cpclub.backend.score.repository;

import com.cpclub.backend.score.entity.PracticeSuggestionSkip;
import com.cpclub.backend.score.entity.PracticeSuggestionSkipId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PracticeSuggestionSkipRepository extends JpaRepository<PracticeSuggestionSkip, PracticeSuggestionSkipId> {
    List<PracticeSuggestionSkip> findByIdUserId(Long userId);
    boolean existsByIdUserIdAndIdProblemId(Long userId, Long problemId);
}
