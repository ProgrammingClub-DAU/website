package com.cpclub.backend.sync.controller;

import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.sync.dto.SyncHealthDto;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import com.cpclub.backend.sync.service.SyncHealthService;
import com.cpclub.backend.sync.service.SyncJobDispatcher;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
@Validated
public class SyncAdminController {

    private final SyncRunRepository syncRunRepository;
    private final SyncHealthService syncHealthService;
    private final SyncJobDispatcher syncJobDispatcher;
    private final MemberScoreService memberScoreService;

    @PostMapping("/scores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> recomputeScores() {
        int updated = memberScoreService.computeScores();
        return ResponseEntity.ok("Successfully computed overall scores for " + updated + " member(s)");
    }

    /**
     * Queues a job. Answers 202 straight away; the run appears in {@code /runs}
     * once the job starts, and 409 when the same job is already running.
     */
    @PostMapping("/{job}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> startJob(@PathVariable SyncJob job) {
        syncJobDispatcher.dispatch(job);
        return ResponseEntity.accepted().body(Map.of("job", job.name(), "status", "QUEUED"));
    }

    @GetMapping("/runs")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SyncRun> getRecentRuns(
            @RequestParam(required = false) SyncJob job,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {
        PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "startedAt"));
        if (job != null) {
            return syncRunRepository.findByJobOrderByStartedAtDesc(job, page);
        }
        return syncRunRepository.findAllByOrderByStartedAtDesc(page);
    }

    @GetMapping("/health")
    @PreAuthorize("hasRole('ADMIN')")
    public List<SyncHealthDto> getHealth() {
        return syncHealthService.getHealth();
    }
}
