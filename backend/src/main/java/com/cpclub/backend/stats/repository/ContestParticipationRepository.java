package com.cpclub.backend.stats.repository;

import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.stats.entity.ContestParticipation;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, Long> {
    void deleteByUserAndPlatform(User user, Platform platform);
    Optional<ContestParticipation> findByUserAndPlatformAndExternalContestId(User user, Platform platform, String externalContestId);
    List<ContestParticipation> findByUserAndPlatform(User user, Platform platform);
}
