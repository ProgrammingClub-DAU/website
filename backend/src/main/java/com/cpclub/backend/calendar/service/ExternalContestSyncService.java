package com.cpclub.backend.calendar.service;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.calendar.service.sources.ClistContestSource;
import com.cpclub.backend.calendar.service.sources.ExternalContestSource;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalContestSyncService {

    @Value("${cpclub.external-contests.enabled:true}")
    private boolean enabled;

    private final List<ExternalContestSource> sources;
    private final ClistContestSource clistContestSource;
    private final ExternalContestRepository repository;
    private final SyncRunRepository syncRunRepository;

    @Scheduled(cron = "0 0 0/6 * * *", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public void sync() {
        if (!enabled) return;
        syncForce();
    }

    @EventListener(condition = "#event.job.name() == 'EXTERNAL_CONTESTS'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        syncForce();
    }

    /**
     * Refreshes every platform and records the whole refresh as one run, counting
     * platforms: "3 / 1" means three calendars refreshed and one failed.
     *
     * <p>It used to write one sync_runs row per platform, all under the same job.
     * Sync Health only looks at a job's latest row, so the job showed whichever
     * platform happened to be synced last, and the run history filled with four
     * rows per refresh.</p>
     */
    public void syncForce() {
        SyncRun run = syncRunRepository.save(SyncRun.builder()
                .job(SyncJob.EXTERNAL_CONTESTS)
                .startedAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        int ok = 0;
        List<String> failures = new ArrayList<>();
        for (ContestPlatform platform : ContestPlatform.values()) {
            try {
                syncPlatform(platform);
                ok++;
            } catch (Exception e) {
                log.error("Failed to sync platform {}: {}", platform, e.getMessage(), e);
                failures.add(platform.name() + ": " + rootMessage(e));
            }
        }

        try {
            repository.deleteByStartsAtLessThan(LocalDateTime.now(ZoneOffset.UTC).minusDays(7));
        } catch (Exception e) {
            log.warn("Could not purge old external contests: {}", e.getMessage());
        }

        run.setMembersOk(ok);
        run.setMembersFailed(failures.size());
        if (!failures.isEmpty()) {
            String sample = String.join("; ", failures);
            run.setErrorSample(sample.length() > 1000 ? sample.substring(0, 1000) : sample);
        }
        run.setFinishedAt(LocalDateTime.now(ZoneOffset.UTC));
        syncRunRepository.save(run);
    }

    /**
     * Replaces one platform's upcoming contests with what the source reports.
     *
     * <p>A source that fails must throw rather than return an empty list: an empty
     * list is read as "no upcoming contests", and every future contest on that
     * platform is deleted.</p>
     */
    public void syncPlatform(ContestPlatform platform) {
        List<ExternalContestDto> upcoming;

        if (clistContestSource.isConfigured() && (platform == ContestPlatform.ATCODER || platform == ContestPlatform.CODECHEF)) {
            upcoming = clistContestSource.fetchUpcoming(platform);
        } else {
            ExternalContestSource source = sources.stream()
                    .filter(s -> s.platform() == platform)
                    .findFirst()
                    .orElse(null);

            if (source == null) {
                return;
            }
            upcoming = source.fetchUpcoming();
        }

        Set<String> fetchedIds = upcoming.stream().map(ExternalContestDto::getId).collect(Collectors.toSet());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (ExternalContestDto dto : upcoming) {
            if (dto.getId() == null || dto.getStartsAt() == null) continue;
            ExternalContest contest = repository.findByPlatformAndExternalId(platform, dto.getId())
                    .orElse(ExternalContest.builder()
                            .platform(platform)
                            .externalId(dto.getId())
                            .build());

            contest.setName(dto.getName());
            contest.setUrl(dto.getUrl());
            contest.setStartsAt(dto.getStartsAt());
            contest.setDurationSeconds(dto.getDurationSeconds());
            contest.setSyncedAt(now);

            repository.save(contest);
        }

        List<ExternalContest> existingFuture = repository.findByPlatform(platform).stream()
                .filter(c -> c.getStartsAt() != null && c.getStartsAt().isAfter(now))
                .collect(Collectors.toList());

        for (ExternalContest existing : existingFuture) {
            if (!fetchedIds.contains(existing.getExternalId())) {
                repository.delete(existing);
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String own = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        if (cause == e || cause.getMessage() == null) return own;
        return own + " (" + cause.getMessage() + ")";
    }
}
