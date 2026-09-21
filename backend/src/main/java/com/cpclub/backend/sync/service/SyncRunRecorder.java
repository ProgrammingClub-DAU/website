package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncRunRecorder {

    private final SyncRunRepository repository;

    public RunHandle start(SyncJob job) {
        try {
            SyncRun run = SyncRun.builder()
                    .job(job)
                    .startedAt(LocalDateTime.now())
                    .build();
            SyncRun saved = repository.save(run);
            return new RunHandle(saved);
        } catch (Exception e) {
            log.error("Failed to start sync run recorder for job {}", job, e);
            return new RunHandle(null);
        }
    }

    public class RunHandle {
        private final SyncRun run;

        private RunHandle(SyncRun run) {
            this.run = run;
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
                    run.setErrorSample(message.length() > 1000 ? message.substring(0, 1000) : message);
                }
            }
        }

        public void finish() {
            if (run != null) {
                try {
                    run.setFinishedAt(LocalDateTime.now());
                    repository.save(run);
                } catch (Exception e) {
                    log.error("Failed to finish sync run recorder for job {}", run.getJob(), e);
                }
            }
        }
    }
}
