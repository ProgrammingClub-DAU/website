package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.dto.SyncHealthDto;
import com.cpclub.backend.sync.dto.SyncHealthDto.Status;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SyncHealthService {

    private final SyncRunRepository syncRunRepository;
    private final SyncJobGuard syncJobGuard;

    /**
     * How often each job is expected to run. A job is stale when nothing has
     * succeeded within twice its interval. CLUB_CONTEST only runs while a club
     * contest is live, so it has no interval and is never stale.
     */
    private static final Map<SyncJob, Duration> INTERVALS = Map.of(
            SyncJob.CF_RATINGS, Duration.ofHours(6),
            SyncJob.CF_SUBMISSIONS, Duration.ofHours(6),
            SyncJob.CF_PROBLEMSET, Duration.ofDays(7),
            SyncJob.CF_CONTESTS, Duration.ofHours(6),
            SyncJob.LC_RATINGS, Duration.ofHours(6),
            SyncJob.LC_CONTESTS, Duration.ofHours(6),
            SyncJob.LC_TOTALS, Duration.ofHours(24),
            SyncJob.EXTERNAL_CONTESTS, Duration.ofHours(6)
    );

    /** A run with no finish time older than this was killed by a restart, not still going. */
    static final Duration ABANDONED_AFTER = Duration.ofHours(2);

    public List<SyncHealthDto> getHealth() {
        List<SyncHealthDto> healthList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (SyncJob job : SyncJob.values()) {
            List<SyncRun> recentRuns = syncRunRepository.findByJobOrderByStartedAtDesc(job, PageRequest.of(0, 50));
            boolean running = syncJobGuard.isRunning(job);
            if (recentRuns.isEmpty() && !running) continue;

            SyncRun lastFinished = recentRuns.stream()
                    .filter(r -> r.getFinishedAt() != null)
                    .findFirst()
                    .orElse(null);
            SyncRun lastSuccessful = recentRuns.stream()
                    .filter(SyncHealthService::succeededForSomeone)
                    .findFirst()
                    .orElse(null);
            SyncRun latest = recentRuns.isEmpty() ? null : recentRuns.get(0);
            boolean inFlight = running || (latest != null && latest.getFinishedAt() == null
                    && latest.getStartedAt().isAfter(now.minus(ABANDONED_AFTER)));

            boolean stale = false;
            Duration interval = INTERVALS.get(job);
            if (interval != null) {
                LocalDateTime limit = now.minus(interval.multipliedBy(2));
                stale = lastSuccessful == null || lastSuccessful.getStartedAt().isBefore(limit);
            }

            SyncRun shown = lastFinished != null ? lastFinished : latest;
            healthList.add(SyncHealthDto.builder()
                    .job(job)
                    .status(statusOf(inFlight, stale, lastFinished))
                    .startedAt(shown != null ? shown.getStartedAt() : null)
                    .finishedAt(shown != null ? shown.getFinishedAt() : null)
                    .membersOk(shown != null ? shown.getMembersOk() : 0)
                    .membersFailed(shown != null ? shown.getMembersFailed() : 0)
                    .errorSample(shown != null ? shown.getErrorSample() : null)
                    .lastSuccessfulRun(lastSuccessful != null ? lastSuccessful.getStartedAt() : null)
                    .stale(stale)
                    .running(inFlight)
                    .build());
        }

        return healthList;
    }

    /**
     * A finished run in which at least one unit succeeded, or nothing failed.
     *
     * <p>Five bad handles out of eighty-one is a problem for those five members, not
     * proof that the job is down; treating it as a failure made every job with one
     * mistyped handle permanently "Stale".</p>
     */
    static boolean succeededForSomeone(SyncRun run) {
        if (run.getFinishedAt() == null) return false;
        if (run.getMembersOk() > 0) return true;
        return run.getMembersFailed() == 0 && run.getErrorSample() == null;
    }

    private static Status statusOf(boolean inFlight, boolean stale, SyncRun lastFinished) {
        if (inFlight) return Status.RUNNING;
        if (lastFinished == null) return Status.NEVER_RUN;
        boolean anyFailed = lastFinished.getMembersFailed() > 0 || lastFinished.getErrorSample() != null;
        if (anyFailed && lastFinished.getMembersOk() == 0) return Status.FAILING;
        if (stale) return Status.STALE;
        if (anyFailed) return Status.DEGRADED;
        return Status.HEALTHY;
    }
}
