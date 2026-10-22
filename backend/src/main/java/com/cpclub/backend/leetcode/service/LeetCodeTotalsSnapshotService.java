package com.cpclub.backend.leetcode.service;

import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.stats.entity.PlatformDailyTotal;
import com.cpclub.backend.stats.repository.PlatformDailyTotalRepository;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Records each member's LeetCode solved totals once a day. Period leaderboards
 * (this week, this month) subtract the snapshot taken on or before the period
 * start from the live total.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeetCodeTotalsSnapshotService {

    /**
     * Totals synced more recently than this are snapshotted as they are. The
     * six-hourly pipeline runs at 00:00 IST and this job at 00:05, so without the
     * check every member was fetched from LeetCode twice within five minutes.
     */
    private static final Duration FRESH_ENOUGH = Duration.ofHours(3);

    private final UserRepository userRepository;
    private final LeetCodeSyncService leetCodeSyncService;
    private final PlatformDailyTotalRepository platformDailyTotalRepository;
    private final SyncRunRecorder syncRunRecorder;
    private final Clock clock; // Injected to resolve timezone correctly (D1, D31)

    /**
     * Not transactional. It used to be, around a loop making one rate-limited HTTP
     * call per member: that held a pooled connection for the whole run, and any
     * exception that crossed the inner transactional call marked the outer one
     * rollback-only, so a single bad member discarded every snapshot of the day.
     */
    @Scheduled(cron = "${cpclub.leetcode.totals-cron:0 5 0 * * *}", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public void captureTotals() {
        LocalDate today = LocalDate.now(clock);
        log.info("Capturing LeetCode daily totals for date: {}", today);
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.LC_TOTALS);
        int ok = 0;
        int failed = 0;
        String errorSample = null;
        LocalDateTime freshAfter = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).minus(FRESH_ENOUGH);

        List<User> users = userRepository.findByLeetcodeHandleIsNotNull();
        for (User user : users) {
            if (user.getLeetcodeHandle() == null || user.getLeetcodeHandle().isBlank()) continue;
            try {
                boolean fresh = user.getLeetcodeSyncedAt() != null && user.getLeetcodeSyncedAt().isAfter(freshAfter);
                if (!fresh) {
                    leetCodeSyncService.syncSingleUser(user);
                }
                if (user.getLeetcodeTotalSolved() == null) {
                    failed++;
                    if (errorSample == null) {
                        errorSample = user.getLeetcodeHandle() + ": no solved totals yet (LeetCode did not answer for this username)";
                    }
                    continue;
                }
                captureSingleUser(user, today);
                ok++;
            } catch (Exception e) {
                log.error("Failed to capture LeetCode totals for user: {}", user.getLeetcodeHandle(), e);
                failed++;
                if (errorSample == null) {
                    errorSample = user.getLeetcodeHandle() + ": " + e.getMessage();
                }
            }
        }
        run.complete(ok, failed, errorSample);
        log.info("Finished capturing LeetCode daily totals: {} ok, {} failed.", ok, failed);
    }

    @Transactional
    public void captureSingleUser(User user) {
        captureSingleUser(user, LocalDate.now(clock));
    }

    @Transactional
    public void captureSingleUser(User user, LocalDate today) {
        if (user.getLeetcodeTotalSolved() != null) {
            if (platformDailyTotalRepository.findByUserAndPlatformAndCapturedOn(user, Platform.LEETCODE.name(), today).isEmpty()) {
                PlatformDailyTotal total = PlatformDailyTotal.builder()
                    .user(user)
                    .platform(Platform.LEETCODE.name())
                    .capturedOn(today)
                    .totalSolved(user.getLeetcodeTotalSolved())
                    .easySolved(user.getLeetcodeEasySolved())
                    .mediumSolved(user.getLeetcodeMediumSolved())
                    .hardSolved(user.getLeetcodeHardSolved())
                    .build();
                platformDailyTotalRepository.save(total);
            }
        }
    }

    @EventListener(condition = "#event.job.name() == 'LC_TOTALS'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        captureTotals();
    }
}
