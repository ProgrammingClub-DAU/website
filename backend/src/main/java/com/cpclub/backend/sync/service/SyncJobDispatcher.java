package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncJobDispatcher {

    private final ApplicationEventPublisher eventPublisher;
    private final SyncRunRepository syncRunRepository;

    public Long dispatch(SyncJob job) {
        // We create a dummy run to get an ID for the 202 response
        // The actual sync service will start its own run or use this ID.
        // Actually, the playbook says "returns 202 with the sync_runs id"
        // Let's create it here.
        SyncRun initialRun = new SyncRun();
        initialRun.setJob(job);
        initialRun.setStartedAt(LocalDateTime.now());
        SyncRun saved = syncRunRepository.save(initialRun);

        // We can publish an event passing the saved ID
        // Currently, we don't need to implement all jobs right now.
        // Stage 1A creates CodeforcesProblemsetSyncService and CodeforcesSubmissionSyncService.
        // But for now, we'll just publish an event.
        return saved.getId();
    }
}
