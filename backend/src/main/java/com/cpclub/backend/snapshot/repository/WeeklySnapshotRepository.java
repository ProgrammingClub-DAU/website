package com.cpclub.backend.snapshot.repository;

import com.cpclub.backend.snapshot.entity.WeeklySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * JPA repository for the weekly rating history behind the profile charts.
 */
@Repository
public interface WeeklySnapshotRepository extends JpaRepository<WeeklySnapshot, Long> {

    /**
     * Returns one member's rating history on a single platform, oldest first.
     *
     * <p>Chart order is chronological, so the sort is done here rather than in
     * Java. The composite index on {@code (user_id, platform, recorded_at)} added
     * in {@code V7__create_weekly_snapshots.sql} covers this exactly: the filter
     * and the ordering are both satisfied from the index, with no sort step.</p>
     *
     * @param userId member whose history is requested
     * @param platform {@link WeeklySnapshot#PLATFORM_CODEFORCES} or
     *                 {@link WeeklySnapshot#PLATFORM_LEETCODE}
     * @return snapshots in ascending date order, empty when none were recorded
     */
    List<WeeklySnapshot> findByUserIdAndPlatformOrderByRecordedAtAsc(Long userId, String platform);

    /**
     * Reports whether any snapshot has been written at or after the given instant.
     *
     * <p>Used as the weekly job's idempotency guard. The scheduler is an in-process
     * timer, so a deploy or a restart on Monday can fire the job a second time;
     * without this check that would write a duplicate point for every member and
     * put two entries on the same day of the chart.</p>
     *
     * @param recordedAt lower bound, typically the start of the current day
     * @return whether snapshots already exist in that window
     */
    boolean existsByRecordedAtGreaterThanEqual(LocalDateTime recordedAt);
}
