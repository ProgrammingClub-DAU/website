package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.dto.SyncHealthDto;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncHealthServiceTest {

    @Mock
    private SyncRunRepository repository;

    @Spy
    private SyncJobGuard syncJobGuard = new SyncJobGuard();

    @InjectMocks
    private SyncHealthService service;

    private static final LocalDateTime NOW = LocalDateTime.now(ZoneOffset.UTC);

    private static SyncRun run(SyncJob job, LocalDateTime started, boolean finished, int ok, int failed, String error) {
        return SyncRun.builder()
                .job(job)
                .startedAt(started)
                .finishedAt(finished ? started.plusMinutes(2) : null)
                .membersOk(ok)
                .membersFailed(failed)
                .errorSample(error)
                .build();
    }

    private SyncHealthDto healthOf(SyncJob job, SyncRun... runs) {
        // The service asks about every job; only this one has history.
        when(repository.findByJobOrderByStartedAtDesc(any(SyncJob.class), any(Pageable.class)))
                .thenAnswer(inv -> inv.getArgument(0) == job ? List.of(runs) : List.of());
        return service.getHealth().stream().filter(h -> h.getJob() == job).findFirst().orElseThrow();
    }

    @Test
    void whenLastSuccessThreeIntervalsAgo_staleIsTrue() {
        SyncHealthDto h = healthOf(SyncJob.CF_SUBMISSIONS,
                run(SyncJob.CF_SUBMISSIONS, NOW.minusHours(1), true, 0, 1, "Failed"),
                run(SyncJob.CF_SUBMISSIONS, NOW.minusHours(19), true, 10, 0, null)); // 6h interval x 3

        assertTrue(h.isStale());
        assertEquals(SyncHealthDto.Status.FAILING, h.getStatus());
    }

    /**
     * The dispatcher used to leave an unfinished, error-free row behind on every
     * Run Now, and health counted it as a success: CF Problemset showed Healthy
     * while every real run failed.
     */
    @Test
    void unfinishedRunWithNoErrorIsNotASuccess() {
        SyncHealthDto h = healthOf(SyncJob.CF_PROBLEMSET,
                run(SyncJob.CF_PROBLEMSET, NOW.minusHours(1), true, 0, 1, "boom"),
                run(SyncJob.CF_PROBLEMSET, NOW.minusDays(1), false, 0, 0, null));

        assertNull(h.getLastSuccessfulRun());
        assertEquals(SyncHealthDto.Status.FAILING, h.getStatus());
    }

    /** Five mistyped handles out of eighty-one is not the job being down. */
    @Test
    void partialFailureIsDegradedNotStale() {
        SyncHealthDto h = healthOf(SyncJob.CF_SUBMISSIONS,
                run(SyncJob.CF_SUBMISSIONS, NOW.minusHours(1), true, 76, 5, "x: not found"));

        assertFalse(h.isStale());
        assertEquals(SyncHealthDto.Status.DEGRADED, h.getStatus());
        assertEquals("x: not found", h.getErrorSample());
    }

    @Test
    void cleanRecentRunIsHealthy() {
        SyncHealthDto h = healthOf(SyncJob.LC_TOTALS,
                run(SyncJob.LC_TOTALS, NOW.minusHours(2), true, 18, 0, null));

        assertEquals(SyncHealthDto.Status.HEALTHY, h.getStatus());
    }

    @Test
    void runningJobIsReportedAsRunning() {
        syncJobGuard.tryStart(SyncJob.CF_CONTESTS);

        SyncHealthDto h = healthOf(SyncJob.CF_CONTESTS,
                run(SyncJob.CF_CONTESTS, NOW.minusHours(3), true, 81, 0, null));

        assertTrue(h.isRunning());
        assertEquals(SyncHealthDto.Status.RUNNING, h.getStatus());
    }

    @Test
    void runKilledByARestartIsNotRunningForever() {
        SyncHealthDto h = healthOf(SyncJob.CF_CONTESTS,
                run(SyncJob.CF_CONTESTS, NOW.minusHours(5), false, 0, 0, null),
                run(SyncJob.CF_CONTESTS, NOW.minusHours(6), true, 81, 0, null));

        assertFalse(h.isRunning());
        assertEquals(SyncHealthDto.Status.HEALTHY, h.getStatus());
    }
}
