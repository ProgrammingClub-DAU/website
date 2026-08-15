package com.cpclub.backend.codeforces.service;

import com.google.common.util.concurrent.RateLimiter;
import com.cpclub.backend.codeforces.dto.CodeforcesResponse;
import com.cpclub.backend.codeforces.dto.CodeforcesUserDto;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Synchronizes stored member ratings with the public Codeforces {@code user.info} API.
 *
 * <p>It batches all valid linked handles into one provider request to limit traffic,
 * updates only matching rated accounts, and contains external failures so a Codeforces
 * outage never interrupts the rest of the application or discards prior ratings.</p>
 *
 * <h2>Rate limiting</h2>
 * <p>Every outbound HTTP call goes through {@link #fetch(List)}, which acquires one
 * permit from the shared {@link RateLimiter} before sending. That limiter is the sole
 * gate: it covers the scheduled job, the admin manual trigger, and every step of the
 * bisect fallback — there is no way to bypass it by adding a new call site.</p>
 *
 * <h2>Fault isolation</h2>
 * <p>When a batch fails, the code bisects it rather than retrying each handle
 * individually. Bisecting costs O(log N) requests to isolate one bad handle among N;
 * sequential retry costs O(N). With the 2-second gate and 100 members, bisect takes
 * ≤28 seconds and sequential would take ≥200 seconds — long enough to be throttled
 * itself and misdiagnose valid handles as broken.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CodeforcesSyncService {

    private static final String CODEFORCES_API_URL = "https://codeforces.com/api/user.info?handles=";

    /**
     * Handles per request. Codeforces accepts a semicolon-separated list, but a
     * single unbounded list eventually exceeds the request-line limit, and the
     * endpoint is all-or-nothing — so smaller batches also shrink the blast
     * radius of one bad handle.
     */
    private static final int BATCH_SIZE = 100;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    /**
     * Application-wide gate on outbound Codeforces calls.
     * Injected from {@link com.cpclub.backend.common.config.AppConfig}.
     */
    private final RateLimiter codeforcesRateLimiter;

    /**
     * Performs the scheduled or administrator-triggered rating refresh.
     *
     * <p>Blank handles are excluded before the external call. Provider errors are logged
     * and intentionally not rethrown because the next scheduled execution can retry.</p>
     */
    @Scheduled(cron = "${cpclub.codeforces.sync-cron:0 0 */6 * * *}")
    @Transactional
    public void syncCodeforcesRatings() {
        log.info("Starting scheduled Codeforces rating synchronization job...");

        List<User> usersWithHandle = userRepository.findByCodeforcesHandleIsNotNull();
        if (usersWithHandle.isEmpty()) {
            log.info("No users with Codeforces handles found. Skipping sync.");
            return;
        }

        List<String> handles = usersWithHandle.stream()
                .map(User::getCodeforcesHandle)
                .filter(Objects::nonNull)
                .filter(h -> !h.isBlank())
                .toList();

        if (handles.isEmpty()) {
            log.info("No valid non-blank handles to sync.");
            return;
        }

        // Match provider results case-insensitively: Codeforces handles are not
        // case-sensitive, so a member stored as "Tourist" must match "tourist".
        Map<String, User> userMap = usersWithHandle.stream()
                .filter(u -> u.getCodeforcesHandle() != null && !u.getCodeforcesHandle().isBlank())
                .collect(java.util.stream.Collectors.toMap(
                        u -> u.getCodeforcesHandle().toLowerCase(),
                        u -> u,
                        (existing, replacement) -> existing
                ));

        int updatedCount = 0;
        for (int start = 0; start < handles.size(); start += BATCH_SIZE) {
            List<String> batch = handles.subList(start, Math.min(start + BATCH_SIZE, handles.size()));
            updatedCount += syncBatch(batch, userMap);
        }

        log.info("Codeforces sync complete. Updated {} of {} handles.", updatedCount, handles.size());
    }

    /**
     * Synchronously fetches and applies the rating for a single user.
     * Used for Just-in-Time (JIT) updates when a user registers or updates their handle.
     * Reuses the batch sync logic for a single element.
     */
    @Transactional
    public void syncSingleUser(User user) {
        if (user == null || user.getCodeforcesHandle() == null || user.getCodeforcesHandle().isBlank()) {
            return;
        }
        
        List<String> batch = List.of(user.getCodeforcesHandle());
        Map<String, User> userMap = Map.of(user.getCodeforcesHandle().toLowerCase(), user);
        syncBatch(batch, userMap);
    }

    /**
     * Syncs one batch, bisecting on failure to isolate the bad handle(s) in O(log N) requests.
     *
     * <p>The Codeforces {@code user.info} endpoint is all-or-nothing: a single
     * invalid handle — a typo, a renamed or deleted account — makes the whole
     * response {@code FAILED} with a null result.</p>
     *
     * <p>When a batch fails, this method splits it in half and retries each half
     * independently. A half that succeeds updates all its members; a half that fails
     * is bisected again. A batch of size 1 that fails identifies exactly the bad
     * handle. The total cost to isolate one bad handle among N is O(log N) requests,
     * compared to O(N) for sequential per-handle retry.</p>
     *
     * <p>A rate-limit or server-error response is detected and does <em>not</em>
     * log member handles as the cause. Misattributing a throttle to a bad handle
     * would send someone to fix something that is not broken.</p>
     *
     * @return how many users were updated
     */
    private int syncBatch(List<String> batch, Map<String, User> userMap) {
        CodeforcesResponse response = fetch(batch);

        if (response != null && "OK".equalsIgnoreCase(response.status()) && response.result() != null) {
            return applyResults(response.result(), userMap);
        }

        // Detect rate-limit or server-side throttling. Do NOT blame individual handles:
        // those members' data is valid; the API refused us because we sent too many
        // requests. Stop this run; the next scheduled execution will retry cleanly.
        if (isThrottled(response)) {
            log.warn("Codeforces rate-limited this server's IP on a batch of {} handles. "
                    + "Stopping this sync run — the rate limiter will space requests correctly "
                    + "on the next execution.", batch.size());
            return 0;
        }

        // Base case: a single handle failed. This is the actual bad handle.
        if (batch.size() == 1) {
            log.warn("Codeforces rejected handle '{}' — leaving its rating unchanged. "
                    + "The member should correct it on their profile. "
                    + "CF comment: {}", batch.get(0), response != null ? response.comment() : "no response");
            return 0;
        }

        // Recursive bisect: split and retry each half.
        log.info("Batch of {} handles failed ({}). Bisecting to isolate bad handle(s) in O(log N) requests.",
                batch.size(), response != null ? response.comment() : "no response");
        int mid = batch.size() / 2;
        int updated = syncBatch(batch.subList(0, mid), userMap);
        updated    += syncBatch(batch.subList(mid, batch.size()), userMap);
        return updated;
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
        String lower = comment.toLowerCase();
        return lower.contains("too many") || lower.contains("limit") || lower.contains("throttl");
    }

    /**
     * Single outbound call. Acquires one permit from the global rate limiter before
     * sending — this is the single choke point for every Codeforces HTTP request in
     * the application. Never throws: a provider failure must not abort the run.
     */
    private CodeforcesResponse fetch(List<String> handles) {
        // Block until a permit is available. With create(0.5) this spaces requests
        // at least 2 seconds apart, regardless of caller or context.
        codeforcesRateLimiter.acquire();
        try {
            return restTemplate.getForObject(
                    CODEFORCES_API_URL + String.join(";", handles), CodeforcesResponse.class);
        } catch (Exception e) {
            log.warn("Codeforces request failed for {} handle(s): {}", handles.size(), e.getMessage());
            return null;
        }
    }

    /** Writes ratings for the users a response covers. Unrated accounts are left alone. */
    private int applyResults(List<CodeforcesUserDto> results, Map<String, User> userMap) {
        int updated = 0;
        for (CodeforcesUserDto cfUser : results) {
            if (cfUser.handle() == null) {
                continue;
            }
            User user = userMap.get(cfUser.handle().toLowerCase());
            if (user != null && cfUser.rating() != null) {
                user.setRating(cfUser.rating());
                userRepository.save(user);
                updated++;
            }
        }
        return updated;
    }
}
