package com.cpclub.backend.user.service;

import com.cpclub.backend.codeforces.service.CodeforcesSubmissionSyncService;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.common.exception.TooManyRequestsException;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.leetcode.service.LeetCodeTotalsSnapshotService;
import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.stats.service.ContestHistorySyncService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Refreshes one member's external stats in the background: rating, accepted
 * solves, contest history, LeetCode totals, then the club's scores.
 *
 * <p>This used to start a {@code new Thread} for every request, and any signed-in
 * member could request one as often as they liked through "Sync now". Each thread
 * runs a backfill of up to a hundred rate-limited calls; a few dozen clicks would
 * exhaust a 512 MB instance. Requests now go to a single worker with a bounded
 * queue, a member already queued is not queued again, and a member may ask once
 * every {@link #COOLDOWN}. A handle change skips the cooldown: there is new data
 * to fetch.</p>
 */
@Slf4j
@Service
public class MemberStatsSyncService {

    static final Duration COOLDOWN = Duration.ofMinutes(10);
    private static final int MAX_QUEUED = 200;

    private final UserRepository userRepository;
    private final CodeforcesSyncService codeforcesSyncService;
    private final CodeforcesSubmissionSyncService codeforcesSubmissionSyncService;
    private final ContestHistorySyncService contestHistorySyncService;
    private final LeetCodeSyncService leetCodeSyncService;
    private final LeetCodeTotalsSnapshotService leetCodeTotalsSnapshotService;
    private final MemberScoreService memberScoreService;
    private final Clock clock;

    private final Set<Long> queued = ConcurrentHashMap.newKeySet();
    private final Map<Long, Instant> lastRequested = new ConcurrentHashMap<>();

    /** One worker: every call it makes queues behind the same per-host rate limiters anyway. */
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_QUEUED),
            runnable -> {
                Thread thread = new Thread(runnable, "member-stats-sync");
                thread.setDaemon(true);
                return thread;
            });

    public MemberStatsSyncService(UserRepository userRepository,
                                  CodeforcesSyncService codeforcesSyncService,
                                  CodeforcesSubmissionSyncService codeforcesSubmissionSyncService,
                                  ContestHistorySyncService contestHistorySyncService,
                                  LeetCodeSyncService leetCodeSyncService,
                                  LeetCodeTotalsSnapshotService leetCodeTotalsSnapshotService,
                                  MemberScoreService memberScoreService,
                                  Clock clock) {
        this.userRepository = userRepository;
        this.codeforcesSyncService = codeforcesSyncService;
        this.codeforcesSubmissionSyncService = codeforcesSubmissionSyncService;
        this.contestHistorySyncService = contestHistorySyncService;
        this.leetCodeSyncService = leetCodeSyncService;
        this.leetCodeTotalsSnapshotService = leetCodeTotalsSnapshotService;
        this.memberScoreService = memberScoreService;
        this.clock = clock;
    }

    /**
     * Queues a refresh requested by the member.
     *
     * @return false when a refresh for this member is already queued or running
     * @throws TooManyRequestsException when the member asked within the cooldown
     */
    public boolean requestByMember(Long userId) {
        Instant now = clock.instant();
        Instant last = lastRequested.get(userId);
        if (last != null && now.isBefore(last.plus(COOLDOWN))) {
            long wait = Duration.between(now, last.plus(COOLDOWN)).toSeconds();
            throw new TooManyRequestsException(
                    "Your stats were refreshed recently. Try again in " + Math.max(1, (wait + 59) / 60) + " minute(s).",
                    wait);
        }
        return enqueue(userId);
    }

    /** Queues a refresh after a handle change. No cooldown. */
    public boolean requestAfterHandleChange(Long userId) {
        return enqueue(userId);
    }

    private boolean enqueue(Long userId) {
        if (!queued.add(userId)) {
            return false;
        }
        lastRequested.put(userId, clock.instant());
        try {
            executor.execute(() -> {
                try {
                    refresh(userId);
                } finally {
                    queued.remove(userId);
                }
            });
            return true;
        } catch (RejectedExecutionException e) {
            queued.remove(userId);
            throw new TooManyRequestsException("Too many refreshes are queued right now. Try again in a few minutes.", 300);
        }
    }

    private void refresh(Long userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;
            log.info("Refreshing external stats for member {} ({})", userId, user.getName());

            boolean hasCf = user.getCodeforcesHandle() != null && !user.getCodeforcesHandle().isBlank();
            boolean hasLc = user.getLeetcodeHandle() != null && !user.getLeetcodeHandle().isBlank();

            if (hasCf) {
                codeforcesSyncService.syncSingleUser(user);
                try {
                    codeforcesSubmissionSyncService.syncMember(user);
                } catch (Exception e) {
                    log.warn("Solve backfill failed for member {}: {}", userId, e.getMessage());
                }
            }
            if (hasLc && leetCodeSyncService.syncSingleUser(user)) {
                leetCodeTotalsSnapshotService.captureSingleUser(user);
            }
            if (hasCf || hasLc) {
                contestHistorySyncService.syncUserContests(user);
                memberScoreService.computeScores();
            }
            log.info("Finished refreshing external stats for member {}", userId);
        } catch (Exception e) {
            log.error("External stats refresh failed for member {}", userId, e);
        }
    }

    public boolean isQueued(Long userId) {
        return queued.contains(userId);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
