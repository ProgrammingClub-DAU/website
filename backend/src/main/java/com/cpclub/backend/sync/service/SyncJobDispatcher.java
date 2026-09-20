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
        // Create the initial run record so we have an ID to return
        SyncRun initialRun = new SyncRun();
        initialRun.setJob(job);
        initialRun.setStartedAt(LocalDateTime.now());
        SyncRun saved = syncRunRepository.save(initialRun);

        // Publish the event in a background thread so the admin HTTP request doesn't block
        new Thread(() -> {
            try {
                eventPublisher.publishEvent(new com.cpclub.backend.sync.event.SyncTriggerEvent(this, job));
            } catch (Exception e) {
                log.error("Failed to execute manual sync job: {}", job, e);
            }
        }).start();

        return saved.getId();
    }
}
