package com.cpclub.backend.codeforces.service;

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
     * Syncs one batch, falling back to individual requests if the batch fails.
     *
     * <p>The Codeforces {@code user.info} endpoint is all-or-nothing: a single
     * invalid handle — a typo, a renamed or deleted account — makes the whole
     * response {@code FAILED} with a null result. Without the fallback below, one
     * member's bad handle silently freezes the leaderboard for the entire club,
     * indefinitely, with only a warning in the log.
     *
     * @return how many users were updated
     */
    private int syncBatch(List<String> batch, Map<String, User> userMap) {
        CodeforcesResponse response = fetch(batch);

        if (response != null && "OK".equalsIgnoreCase(response.status()) && response.result() != null) {
            return applyResults(response.result(), userMap);
        }

        if (batch.size() == 1) {
            // Already isolated: this specific handle is the bad one.
            log.warn("Codeforces rejected handle '{}' — leaving its rating unchanged. "
                    + "The member should correct it on their profile.", batch.get(0));
            return 0;
        }

        log.warn("Batch of {} handles failed ({}). Retrying individually to isolate the bad handle(s).",
                batch.size(), response != null ? response.comment() : "no response");

        int updated = 0;
        for (String handle : batch) {
            updated += syncBatch(List.of(handle), userMap);
        }
        return updated;
    }

    /** Single outbound call. Never throws: a provider failure must not abort the run. */
    private CodeforcesResponse fetch(List<String> handles) {
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
