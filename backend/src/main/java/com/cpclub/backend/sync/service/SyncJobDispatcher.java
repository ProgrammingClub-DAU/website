package com.cpclub.backend.sync.service;

import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs admin-triggered sync jobs in the background.
 *
 * <p>Each job records its own sync_runs row. This class used to insert one more
 * "initial" row per trigger that nothing ever finished, so every Run Now left a
 * phantom run behind -- and because it had no error sample, Sync Health counted
 * it as the job's last success. That is how a CF Problemset sync that had never
 * succeeded showed as Healthy.</p>
 */
@Slf4j
@Service
public class SyncJobDispatcher {

    /** Jobs whose results feed member_scores, which is recomputed once they finish. */
    private static final Set<SyncJob> SCORE_INPUTS = EnumSet.of(
            SyncJob.CF_RATINGS, SyncJob.CF_SUBMISSIONS, SyncJob.CF_CONTESTS,
            SyncJob.LC_RATINGS, SyncJob.LC_CONTESTS, SyncJob.LC_TOTALS);

    private final ApplicationEventPublisher eventPublisher;
    private final SyncJobGuard syncJobGuard;
    private final MemberScoreService memberScoreService;

    /**
     * Two workers, so a long Codeforces backfill does not hold up a calendar
     * refresh. Bounded, unlike the {@code new Thread} per click it replaces.
     */
    private final ExecutorService executor = new ThreadPoolExecutor(
            2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(SyncJob.values().length),
            namedDaemonThreads());

    public SyncJobDispatcher(ApplicationEventPublisher eventPublisher,
                             SyncJobGuard syncJobGuard,
                             MemberScoreService memberScoreService) {
        this.eventPublisher = eventPublisher;
        this.syncJobGuard = syncJobGuard;
        this.memberScoreService = memberScoreService;
    }

    /**
     * Queues {@code job} to run in the background.
     *
     * @throws ConflictException when the job is already queued or running
     */
    public void dispatch(SyncJob job) {
        if (!syncJobGuard.tryStart(job)) {
            throw new ConflictException(job + " is already running. Wait for it to finish before starting it again.");
        }
        try {
            executor.execute(() -> run(job));
        } catch (RejectedExecutionException e) {
            syncJobGuard.finish(job);
            throw new ConflictException("Too many sync jobs are queued. Try again in a few minutes.");
        }
    }

    private void run(SyncJob job) {
        try {
            eventPublisher.publishEvent(new SyncTriggerEvent(this, job));
            if (SCORE_INPUTS.contains(job)) {
                memberScoreService.computeScores();
            }
        } catch (Exception e) {
            log.error("Manual sync job {} failed", job, e);
        } finally {
            syncJobGuard.finish(job);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    private static java.util.concurrent.ThreadFactory namedDaemonThreads() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "sync-dispatch-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
