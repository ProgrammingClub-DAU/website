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
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeetCodeTotalsSnapshotService {

    private final UserRepository userRepository;
    private final LeetCodeSyncService leetCodeSyncService;
    private final PlatformDailyTotalRepository platformDailyTotalRepository;
    private final SyncRunRecorder syncRunRecorder;
    private final Clock clock; // Injected to resolve timezone correctly (D1, D31)

    @Scheduled(cron = "${cpclub.leetcode.totals-cron:0 5 0 * * *}", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    @Transactional
    public void captureTotals() {
        LocalDate today = LocalDate.now(clock);
        log.info("Capturing LeetCode daily totals for date: {}", today);
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.LC_TOTALS);
        int ok = 0;
        int failed = 0;
        String errorSample = null;
        
        List<User> users = userRepository.findByLeetcodeHandleIsNotNull();
        for (User user : users) {
            try {
                // Re-fetch to get the most up-to-date live totals
                leetCodeSyncService.syncSingleUser(user);
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
        log.info("Finished capturing LeetCode daily totals.");
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
