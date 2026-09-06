package com.cpclub.backend.snapshot.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.snapshot.dto.RatingPointDto;
import com.cpclub.backend.snapshot.entity.WeeklySnapshot;
import com.cpclub.backend.snapshot.repository.WeeklySnapshotRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Records and serves the weekly rating history shown on member profiles.
 *
 * <h2>Why the job never calls an external API</h2>
 * <p>The Monday job snapshots the rating values already stored on {@code users};
 * it does not re-fetch from Codeforces or LeetCode. Those are kept fresh by their
 * own sync jobs. Keeping the snapshot free of network calls means a provider
 * outage costs one flat week in the chart rather than a failed job, a partial
 * write, or a long-running transaction holding connections on a pool capped at
 * three.</p>
 *
 * <h2>Duplicate protection</h2>
 * <p>{@code @Scheduled} is an in-process timer with no distributed lock, so a
 * restart or redeploy on a Monday can fire the job twice. The guard in
 * {@link #recordWeeklySnapshots()} makes a second run in the same day a no-op.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {

    private final WeeklySnapshotRepository weeklySnapshotRepository;
    private final UserRepository userRepository;

    /**
     * Captures every member's current Codeforces and LeetCode rating.
     *
     * <p>Runs at midnight each Monday by default. A member contributes one row per
     * platform they actually have a rating for, so an unrated account adds nothing
     * and leaves no gap in the other platform's series.</p>
     *
     * <p>Members are read with {@code findAll()} and filtered in memory rather than
     * through a narrower query. That is deliberate: the narrower query would belong
     * on {@code UserRepository}, which M5 is editing in Stage 1B, and a second
     * parallel branch touching the same file invites a merge conflict for no real
     * gain — this runs once a week over a club-sized table. Worth revisiting once
     * Stage 1 is merged.</p>
     */
    @Scheduled(cron = "${cpclub.snapshot.cron:0 0 0 * * MON}")
    @Transactional
    public void recordWeeklySnapshots() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        if (weeklySnapshotRepository.existsByRecordedAtGreaterThanEqual(startOfToday)) {
            log.info("Weekly snapshots already recorded today; skipping this run.");
            return;
        }

        List<WeeklySnapshot> pending = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (user.getRating() != null) {
                pending.add(build(user, WeeklySnapshot.PLATFORM_CODEFORCES, user.getRating()));
            }
            if (user.getLeetcodeRating() != null) {
                pending.add(build(user, WeeklySnapshot.PLATFORM_LEETCODE, user.getLeetcodeRating()));
            }
        }

        if (pending.isEmpty()) {
            log.info("Weekly snapshot run found no rated members; nothing recorded.");
            return;
        }

        weeklySnapshotRepository.saveAll(pending);
        log.info("Recorded {} weekly rating snapshot(s).", pending.size());
    }

    /**
     * Returns one member's Codeforces rating history, oldest point first.
     *
     * @param userId member whose history is requested
     * @return chart points, empty when no snapshot has been taken yet
     * @throws ResourceNotFoundException when no such member exists
     */
    @Transactional(readOnly = true)
    public List<RatingPointDto> getCodeforcesHistory(Long userId) {
        return getHistory(userId, WeeklySnapshot.PLATFORM_CODEFORCES);
    }

    /**
     * Returns one member's LeetCode rating history, oldest point first.
     *
     * @param userId member whose history is requested
     * @return chart points, empty when no snapshot has been taken yet
     * @throws ResourceNotFoundException when no such member exists
     */
    @Transactional(readOnly = true)
    public List<RatingPointDto> getLeetcodeHistory(Long userId) {
        return getHistory(userId, WeeklySnapshot.PLATFORM_LEETCODE);
    }

    /**
     * Loads and maps one platform's history for a member.
     *
     * <p>A member with no snapshots yet returns an empty list, not a 404 — the
     * chart renders as empty rather than as an error. An unknown member id is a
     * genuine 404, so a mistyped URL is distinguishable from a new account.</p>
     */
    private List<RatingPointDto> getHistory(Long userId, String platform) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return weeklySnapshotRepository
                .findByUserIdAndPlatformOrderByRecordedAtAsc(userId, platform)
                .stream()
                .map(RatingPointDto::fromEntity)
                .toList();
    }

    /**
     * Builds one snapshot row. {@code recordedAt} is left to
     * {@code @CreationTimestamp} so every row in a run shares the persist time.
     */
    private WeeklySnapshot build(User user, String platform, Integer rating) {
        return WeeklySnapshot.builder()
                .user(user)
                .platform(platform)
                .rating(rating)
                .build();
    }
}
