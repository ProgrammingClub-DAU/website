package com.cpclub.backend.codeforces.service;

import com.google.common.util.concurrent.RateLimiter;
import com.cpclub.backend.codeforces.dto.CodeforcesResponse;
import com.cpclub.backend.codeforces.dto.CodeforcesUserDto;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncJobGuard;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.stats.service.ContestHistorySyncService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Synchronizes stored member ratings with the public Codeforces {@code user.info} API,
 * and drives the six-hourly platform pipeline.
 *
 * <p>It batches all valid linked handles into one provider request to limit traffic,
 * updates only matching rated accounts, and contains external failures so a Codeforces
 * outage never interrupts the rest of the application or discards prior ratings.</p>
 *
 * <h2>Rate limiting</h2>
 * <p>Every outbound HTTP call goes through {@link #fetch(List)}, which acquires one
 * permit from the shared {@link RateLimiter} before sending. That limiter is the sole
 * gate: it covers the scheduled job, the admin manual trigger, and every step of the
 * fallback below -- there is no way to bypass it by adding a new call site.</p>
 *
 * <h2>Fault isolation</h2>
 * <p>{@code user.info} is all-or-nothing: one bad handle fails the batch. When
 * Codeforces names the missing handle in its comment ("User with handle X not
 * found"), that handle is dropped and the rest retried in one more request. When it
 * does not, the batch is bisected, which isolates one bad handle among N in O(log N)
 * requests rather than the O(N) of retrying each handle on its own.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesSyncService {

    private static final String CODEFORCES_API_URL = "https://codeforces.com/api/user.info?handles=";

    private static final Pattern MISSING_HANDLE = Pattern.compile("User with handle (\\S+) not found");

    /**
     * Handles per request. Codeforces accepts a semicolon-separated list, but a
     * single unbounded list eventually exceeds the request-line limit, and the
     * endpoint is all-or-nothing -- so smaller batches also shrink the blast
     * radius of one bad handle.
     */
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    /**
     * Chained off the end of this job rather than carrying their own schedules.
     * The syncs queue behind the same rate limiters, so separate cron expressions
     * would only have them block each other.
     */
    private final LeetCodeSyncService leetCodeSyncService;
    private final CodeforcesSubmissionSyncService codeforcesSubmissionSyncService;
    private final ContestHistorySyncService contestHistorySyncService;
    private final MemberScoreService memberScoreService;
    private final SyncRunRecorder syncRunRecorder;
    private final SyncJobGuard syncJobGuard;

    /**
     * Application-wide gate on outbound Codeforces calls.
     * Injected from {@link com.cpclub.backend.common.config.AppConfig}.
     */
    private final RateLimiter codeforcesRateLimiter;

    /**
     * The six-hourly pipeline: ratings, solves, contest history, then scores.
     *
     * <p>Each step is guarded, so a step an admin already started by hand is
     * skipped rather than run twice at once. Provider errors are recorded in
     * sync_runs and never rethrown: the next scheduled run retries.</p>
     */
    @Scheduled(cron = "${cpclub.codeforces.sync-cron:0 0 */6 * * *}", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    public void syncCodeforcesRatings() {
        log.info("Starting scheduled platform synchronization pipeline...");

        runStep(SyncJob.CF_RATINGS, this::syncCodeforcesHandles);
        runStep(SyncJob.CF_SUBMISSIONS, codeforcesSubmissionSyncService::syncAll);
        runStep(SyncJob.LC_RATINGS, leetCodeSyncService::syncAllUsers);
        runStep(SyncJob.CF_CONTESTS, contestHistorySyncService::syncAllCodeforcesContests);
        runStep(SyncJob.LC_CONTESTS, contestHistorySyncService::syncAllLeetCodeContests);

        try {
            memberScoreService.computeScores();
        } catch (Exception e) {
            log.error("Failed to recompute member scores: {}", e.getMessage());
        }

        log.info("Platform synchronization pipeline finished.");
    }

    private void runStep(SyncJob job, Runnable step) {
        try {
            syncJobGuard.runExclusive(job, step);
        } catch (Exception e) {
            log.error("Pipeline step {} failed: {}", job, e.getMessage(), e);
        }
    }

    @EventListener(condition = "#event.job.name() == 'CF_RATINGS'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        syncCodeforcesHandles();
    }

    /**
     * Refreshes every linked member's current and peak rating, and records the run
     * as {@link SyncJob#CF_RATINGS}.
     */
    public void syncCodeforcesHandles() {
        log.info("Starting Codeforces rating synchronization...");
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.CF_RATINGS);

        List<User> usersWithHandle = userRepository.findByCodeforcesHandleIsNotNull();
        List<String> handles = usersWithHandle.stream()
                .map(User::getCodeforcesHandle)
                .filter(Objects::nonNull)
                .filter(h -> !h.isBlank())
                .toList();

        if (handles.isEmpty()) {
            log.info("No valid non-blank handles to sync.");
            run.complete(0, 0, null);
            return;
        }

        // Match provider results case-insensitively: Codeforces handles are not
        // case-sensitive, so a member stored as "Tourist" must match "tourist".
        Map<String, User> userMap = usersWithHandle.stream()
                .filter(u -> u.getCodeforcesHandle() != null && !u.getCodeforcesHandle().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        u -> u.getCodeforcesHandle().toLowerCase(Locale.ROOT),
                        u -> u,
                        (existing, replacement) -> existing
                ));

        BatchOutcome outcome = new BatchOutcome();
        for (int start = 0; start < handles.size(); start += BATCH_SIZE) {
            List<String> batch = handles.subList(start, Math.min(start + BATCH_SIZE, handles.size()));
            syncBatch(new ArrayList<>(batch), userMap, outcome);
        }

        log.info("Codeforces sync complete. Updated {} of {} handles; {} rejected.",
                outcome.updated, handles.size(), outcome.failed);
        run.complete(handles.size() - outcome.failed, outcome.failed, outcome.firstError);
    }

    /**
     * Synchronously fetches and applies the rating for a single user.
     * Used for Just-in-Time (JIT) updates when a user registers or updates their handle.
     * Not transactional: the write is a single targeted update, and a transaction
     * around the HTTP call would hold a pooled connection while Codeforces answers.
     */
    public void syncSingleUser(User user) {
        if (user == null || user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            return;
        }

        List<String> batch = new ArrayList<>(List.of(user.getCodeforcesHandle()));
        Map<String, User> userMap = Map.of(user.getCodeforcesHandle().toLowerCase(Locale.ROOT), user);
        syncBatch(batch, userMap, new BatchOutcome());
    }

    /** Running totals for one sync, shared across batches and recursive calls. */
    private static final class BatchOutcome {
        int updated;
        int failed;
        String firstError;

        void reject(String handle, String reason) {
            failed++;
            if (firstError == null) {
                firstError = handle + ": " + reason;
            }
        }
    }

    /**
     * Syncs one batch, isolating bad handles on failure.
     *
     * <p>A rate-limit or server-error response is detected and does <em>not</em>
     * mark member handles as the cause. Misattributing a throttle to a bad handle
     * would send someone to fix something that is not broken.</p>
     */
    private void syncBatch(List<String> batch, Map<String, User> userMap, BatchOutcome outcome) {
        if (batch.isEmpty()) {
            return;
        }
        CodeforcesResponse response = fetch(batch);

        if (response != null && "OK".equalsIgnoreCase(response.status()) && response.result() != null) {
            outcome.updated += applyResults(response.result(), userMap);
            return;
        }

        String comment = response != null && response.comment() != null ? response.comment() : "no response from Codeforces";

        if (isThrottled(response)) {
            log.warn("Codeforces rate-limited this server's IP on a batch of {} handles. "
                    + "Leaving their ratings unchanged until the next run.", batch.size());
            for (String handle : batch) {
                outcome.reject(handle, "Codeforces rate limit: " + comment);
            }
            return;
        }

        // Base case: a single handle failed. This is the actual bad handle.
        if (batch.size() == 1) {
            log.warn("Codeforces rejected handle '{}' -- leaving its rating unchanged. "
                    + "The member should correct it on their profile. CF comment: {}", batch.get(0), comment);
            outcome.reject(batch.get(0), comment);
            return;
        }

        // Codeforces usually names the handle it could not find. Drop just that one.
        Matcher missing = MISSING_HANDLE.matcher(comment);
        if (missing.find()) {
            String bad = missing.group(1);
            List<String> rest = batch.stream().filter(h -> !h.equalsIgnoreCase(bad)).toList();
            if (rest.size() < batch.size()) {
                log.warn("Codeforces has no user '{}' -- leaving that member's rating unchanged.", bad);
                outcome.reject(bad, comment);
                syncBatch(new ArrayList<>(rest), userMap, outcome);
                return;
            }
        }

        log.info("Batch of {} handles failed ({}). Bisecting to isolate bad handle(s).", batch.size(), comment);
        int mid = batch.size() / 2;
        syncBatch(new ArrayList<>(batch.subList(0, mid)), userMap, outcome);
        syncBatch(new ArrayList<>(batch.subList(mid, batch.size())), userMap, outcome);
    }

    /**
     * Returns true if the Codeforces response signals rate limiting or server-side
     * throttling rather than a bad handle. A throttle must not be logged as the
     * member's fault.
     */
    private boolean isThrottled(CodeforcesResponse response) {
        if (response == null) return false;
        String comment = response.comment();
        if (comment == null) return false;
        String lower = comment.toLowerCase(Locale.ROOT);
        return lower.contains("too many") || lower.contains("limit") || lower.contains("throttl");
    }

    /**
     * Single outbound call. Acquires one permit from the global rate limiter before
     * sending -- this is the single choke point for every user.info request.
     * Never throws: a provider failure must not abort the run.
     */
    private CodeforcesResponse fetch(List<String> handles) {
        codeforcesRateLimiter.acquire();
        try {
            return restTemplate.getForObject(
                    CODEFORCES_API_URL + String.join(";", handles), CodeforcesResponse.class);
        } catch (Exception e) {
            log.warn("Codeforces request failed for {} handle(s): {}", handles.size(), e.getMessage());
            return null;
        }
    }

    /**
     * Writes current and peak rating for the users a response covers.
     *
     * <p>Peak rating was fetched and thrown away before, so {@code max_rating} was
     * never written: every "Peak" on the site showed the current rating, and a member
     * whose rating had dropped could not equip a banner they had earned.</p>
     *
     * <p>Unrated accounts are left alone.</p>
     */
    private int applyResults(List<CodeforcesUserDto> results, Map<String, User> userMap) {
        int updated = 0;
        for (CodeforcesUserDto cfUser : results) {
            if (cfUser.handle() == null) {
                continue;
            }
            User user = userMap.get(cfUser.handle().toLowerCase(Locale.ROOT));
            if (user != null && cfUser.rating() != null) {
                user.setRating(cfUser.rating());
                user.setMaxRating(cfUser.maxRating() != null ? cfUser.maxRating() : cfUser.rating());
                // A targeted update, not save(user): see UserRepository's sync writes.
                userRepository.updateCodeforcesRating(
                        user.getId(), user.getCodeforcesHandle(), user.getRating(), user.getMaxRating());
                updated++;
            }
        }
        return updated;
    }
}
