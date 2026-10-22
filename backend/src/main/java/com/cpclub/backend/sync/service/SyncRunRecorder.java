package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Writes one sync_runs row per job execution.
 *
 * <p>Times are UTC (D31). They used to be {@code LocalDateTime.now()}, the JVM's
 * zone, while {@link SyncHealthService} compared them against UTC -- harmless on
 * Render, five and a half hours off on any machine set to IST.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncRunRecorder {

    private static final int MAX_ERROR_LENGTH = 1000;

    private final SyncRunRepository repository;

    public RunHandle start(SyncJob job) {
        try {
            SyncRun run = SyncRun.builder()
                    .job(job)
                    .startedAt(nowUtc())
                    .build();
            return new RunHandle(repository, repository.save(run));
        } catch (Exception e) {
            log.error("Failed to start sync run recorder for job {}", job, e);
            return RunHandle.detached();
        }
    }

    static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /**
     * An open run. Counters can be bumped one member at a time ({@link #ok},
     * {@link #failed}) and closed with {@link #finish}, or set in one go with
     * {@link #complete}.
     */
    public static final class RunHandle {
        private final SyncRunRepository repository;
        private final SyncRun run;

        private RunHandle(SyncRunRepository repository, SyncRun run) {
            this.repository = repository;
            this.run = run;
        }

        /** A handle that records nothing; used when the run row could not be written, and by tests. */
        public static RunHandle detached() {
            return new RunHandle(null, null);
        }

        public void ok() {
            if (run != null) {
                run.setMembersOk(run.getMembersOk() + 1);
            }
        }

        public void failed(String message) {
            if (run != null) {
                run.setMembersFailed(run.getMembersFailed() + 1);
                if (run.getErrorSample() == null && message != null) {
                    run.setErrorSample(truncate(message));
                }
            }
        }

        public void complete(int ok, int failed, String errorSample) {
            if (run != null) {
                run.setMembersOk(ok);
                run.setMembersFailed(failed);
                if (errorSample != null) {
                    run.setErrorSample(truncate(errorSample));
                }
                finish();
            }
        }

        public void finish() {
            if (run != null) {
                try {
                    run.setFinishedAt(nowUtc());
                    repository.save(run);
                } catch (Exception e) {
                    log.error("Failed to finish sync run recorder for job {}", run.getJob(), e);
                }
            }
        }

        private static String truncate(String message) {
            return message.length() > MAX_ERROR_LENGTH ? message.substring(0, MAX_ERROR_LENGTH) : message;
        }
    }
}
