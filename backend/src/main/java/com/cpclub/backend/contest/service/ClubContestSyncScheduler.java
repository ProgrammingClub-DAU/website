package com.cpclub.backend.contest.service;

import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * The 15-minute tick that keeps live and recently finished club contests in sync.
 *
 * <p>Each contest is synced on its own: {@link ClubContestSyncService#sync} commits
 * per contest, so one failing contest -- Codeforces answering 503 mid-round, say --
 * cannot roll back the others or stop the rest of the tick.</p>
 *
 * <p>Nothing is recorded when nothing is due. Club contests are occasional, so a
 * sync_runs row every 15 minutes saying "nothing to do" would bury the runs that
 * matter in Sync Health.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClubContestSyncScheduler {

    /**
     * Candidates are contests that started within this window. Generous, because a
     * gym contest can run for weeks; the exact rule is {@code needsSync}.
     */
    private static final int CANDIDATE_WINDOW_DAYS = 60;

    private final ClubContestRepository contestRepository;
    private final ClubContestSyncService syncService;
    private final SyncRunRecorder syncRunRecorder;
    private final Clock clock;

    @Scheduled(cron = "0 */15 * * * *", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public void syncDueContests() {
        syncDueContests(false);
    }

    /**
     * @param recordWhenIdle record a run even when nothing is due. A manual trigger
     *        does, so the admin who pressed Run Now sees that it ran; the 15-minute
     *        tick does not, for the reason in the class comment.
     */
    void syncDueContests(boolean recordWhenIdle) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<ClubContest> due = contestRepository
                .findSyncCandidates(now, now.minusDays(CANDIDATE_WINDOW_DAYS)).stream()
                .filter(c -> ClubContestSyncService.needsSync(
                        c.getStartsAt(), c.getDurationSeconds(), c.getLastSyncedAt(), now))
                .toList();
        if (due.isEmpty()) {
            if (recordWhenIdle) {
                syncRunRecorder.start(SyncJob.CLUB_CONTEST).complete(0, 0, null);
            }
            return;
        }

        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.CLUB_CONTEST);
        for (ClubContest contest : due) {
            try {
                syncService.sync(contest.getId());
                run.ok();
            } catch (Exception e) {
                // One contest failing (a Codeforces 503 mid-round, say) must not stop the rest.
                log.warn("Club contest {} sync failed: {}", contest.getId(), e.getMessage());
                run.failed("Club contest " + contest.getId() + ": " + e.getMessage());
            }
        }
        run.finish();
    }

    /** {@code POST /api/admin/sync/CLUB_CONTEST} runs the same check on demand. */
    @EventListener(condition = "#event.job.name() == 'CLUB_CONTEST'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        syncDueContests(true);
    }
}
