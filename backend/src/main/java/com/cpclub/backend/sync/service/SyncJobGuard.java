package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.entity.SyncJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lets at most one run of each sync job execute at a time.
 *
 * <p>Without it an admin double-clicking "Run Now", or pressing it while the
 * six-hourly pipeline is mid-way through the same job, starts two copies that
 * queue behind the same rate limiter, double the run time, and race each other
 * writing the same rows.</p>
 */
@Slf4j
@Component
public class SyncJobGuard {

    private final Set<SyncJob> running = ConcurrentHashMap.newKeySet();

    /** @return true when the caller now owns the job and must call {@link #finish} */
    public boolean tryStart(SyncJob job) {
        return running.add(job);
    }

    public void finish(SyncJob job) {
        running.remove(job);
    }

    public boolean isRunning(SyncJob job) {
        return running.contains(job);
    }

    public Set<SyncJob> running() {
        return running.isEmpty() ? EnumSet.noneOf(SyncJob.class) : EnumSet.copyOf(running);
    }

    /**
     * Runs {@code task} unless the same job is already running, in which case the
     * call is skipped and logged. Used by the scheduled pipeline.
     *
     * @return true when the task ran
     */
    public boolean runExclusive(SyncJob job, Runnable task) {
        if (!tryStart(job)) {
            log.info("Skipping {}: a run is already in progress", job);
            return false;
        }
        try {
            task.run();
            return true;
        } finally {
            finish(job);
        }
    }
}
