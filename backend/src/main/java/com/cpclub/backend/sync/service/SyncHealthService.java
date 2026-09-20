package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.dto.SyncHealthDto;
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

    private static final Map<SyncJob, Duration> INTERVALS = Map.of(
            SyncJob.CF_SUBMISSIONS, Duration.ofHours(6),
            SyncJob.CF_PROBLEMSET, Duration.ofHours(24),
            SyncJob.CF_CONTESTS, Duration.ofHours(6),
            SyncJob.LC_CONTESTS, Duration.ofHours(6),
            SyncJob.LC_TOTALS, Duration.ofHours(24),
            SyncJob.EXTERNAL_CONTESTS, Duration.ofHours(6)
    );

    public List<SyncHealthDto> getHealth() {
        List<SyncHealthDto> healthList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (SyncJob job : SyncJob.values()) {
            List<SyncRun> recentRuns = syncRunRepository.findByJobOrderByStartedAtDesc(job, PageRequest.of(0, 50));
            if (recentRuns.isEmpty()) continue;

            SyncRun lastRun = recentRuns.get(0);
            SyncRun lastSuccessful = recentRuns.stream()
                    .filter(r -> r.getErrorSample() == null)
                    .findFirst()
                    .orElse(null);

            boolean stale = false;
            if (INTERVALS.containsKey(job)) {
                Duration interval = INTERVALS.get(job);
                LocalDateTime limit = now.minus(interval.multipliedBy(2));
                if (lastSuccessful == null || lastSuccessful.getStartedAt().isBefore(limit)) {
                    stale = true;
                }
            }

            healthList.add(SyncHealthDto.builder()
                    .job(job)
                    .startedAt(lastRun.getStartedAt())
                    .finishedAt(lastRun.getFinishedAt())
                    .membersOk(lastRun.getMembersOk())
                    .membersFailed(lastRun.getMembersFailed())
                    .lastSuccessfulRun(lastSuccessful != null ? lastSuccessful.getStartedAt() : null)
                    .stale(stale)
                    .build());
        }

        return healthList;
    }
}
