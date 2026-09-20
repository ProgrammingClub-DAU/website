package com.cpclub.backend.sync.controller;

import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class SyncAdminController {

    private final SyncRunRepository syncRunRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.cpclub.backend.sync.service.SyncHealthService syncHealthService;

    private final com.cpclub.backend.sync.service.SyncJobDispatcher syncJobDispatcher;

    @PostMapping("/{job}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> startJob(@PathVariable SyncJob job) {
        Long runId = syncJobDispatcher.dispatch(job);
        return ResponseEntity.accepted().body(runId);
    }

    @GetMapping("/runs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SyncRun> getRecentRuns(
            @RequestParam(required = false) SyncJob job,
            @RequestParam(defaultValue = "20") int limit) {
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"));
        if (job != null) {
            return syncRunRepository.findByJobOrderByStartedAtDesc(job, page);
        }
        return syncRunRepository.findAllByOrderByStartedAtDesc(page);
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public List<com.cpclub.backend.sync.dto.SyncHealthDto> getHealth() {
        return syncHealthService.getHealth();
    }
}
