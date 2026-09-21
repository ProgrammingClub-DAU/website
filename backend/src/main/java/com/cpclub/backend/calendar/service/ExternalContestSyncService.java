package com.cpclub.backend.calendar.service;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.calendar.service.sources.ClistContestSource;
import com.cpclub.backend.calendar.service.sources.ExternalContestSource;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalContestSyncService {

    @Value("${cpclub.external-contests.enabled:false}")
    private boolean enabled;

    private final List<ExternalContestSource> sources;
    private final ClistContestSource clistContestSource;
    private final ExternalContestRepository repository;
    private final SyncRunRepository syncRunRepository;

    @Scheduled(cron = "0 0 0/6 * * *", zone = "Asia/Kolkata")
    public void sync() {
        if (!enabled) return;
        
        for (ContestPlatform platform : ContestPlatform.values()) {
            SyncRun run = SyncRun.builder()
                    .job(SyncJob.EXTERNAL_CONTESTS)
                    .startedAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build();
            run = syncRunRepository.save(run);
            
            try {
                syncPlatform(platform);
                run.setMembersOk(1);
            } catch (Exception e) {
                log.error("Failed to sync platform: " + platform, e);
                run.setMembersFailed(1);
                run.setErrorSample(platform.name() + ": " + e.getMessage());
            } finally {
                run.setFinishedAt(LocalDateTime.now(ZoneOffset.UTC));
                syncRunRepository.save(run);
            }
        }
        
        repository.deleteByStartsAtLessThan(LocalDateTime.now(ZoneOffset.UTC).minusDays(7));
    }

    @Transactional
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
                .filter(c -> c.getStartsAt().isAfter(now))
                .collect(Collectors.toList());

        for (ExternalContest existing : existingFuture) {
            if (!fetchedIds.contains(existing.getExternalId())) {
                repository.delete(existing);
            }
        }
    }
}
