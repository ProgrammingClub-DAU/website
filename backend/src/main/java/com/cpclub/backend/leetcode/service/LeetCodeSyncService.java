package com.cpclub.backend.leetcode.service;

import com.cpclub.backend.leetcode.dto.LeetCodeGraphQLResponse;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.event.SyncTriggerEvent;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Keeps members' LeetCode contest ratings up to date.
 *
 * <p>LeetCode publishes no REST API. The only public surface is the same GraphQL
 * endpoint the website itself uses, which is undocumented and unversioned â€” so
 * this service is written to degrade rather than to trust it. Every failure path
 * logs and returns; none of them throw.</p>
 *
 * <p>That is a deliberate contract, not laziness. {@code syncSingleUser} is called
 * from {@code UserService.updateProfile()} inside the member's own save. If a
 * LeetCode outage threw from here, a member could not save their name, their
 * GitHub link, or anything else, because a third party was down. Saving the
 * profile is the operation the member asked for; refreshing a rating is a bonus
 * attached to it.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LeetCodeSyncService {

    private static final String LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql";

    /**
     * Fetches only the rating. LeetCode's schema exposes a good deal more on this
     * type; asking for the one field keeps the response small and means a change
     * to any other field cannot break the parse.
     */
    private static final String CONTEST_RANKING_QUERY = """
            query userContestRankingInfo($username: String!) {
              userContestRanking(username: $username) {
                rating
              }
              matchedUser(username: $username) {
                submitStats {
                  acSubmissionNum {
                    difficulty
                    count
                  }
                }
              }
            }
            """;

    /**
     * LeetCode's edge turns away the default Java client far more often than a
     * browser; the contest-history sync already sent this.
     */
    public static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /** Recorded when a handle is valid but has never entered a rated contest. */
    private static final int UNRATED = 0;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    /**
     * Dedicated LeetCode outbound gate (D23).
     *
     * <p>Previously this service shared {@code codeforcesRateLimiter}, which meant
     * a long CF submission backfill blocked all LeetCode calls. Each host now has
     * its own limiter so the two syncs are independent.</p>
     */
    @Qualifier("leetcodeRateLimiter")
    private final RateLimiter leetcodeRateLimiter;

    private final SyncRunRecorder syncRunRecorder;


    /**
     * Refreshes one member's rating, on demand.
     *
     * <p>Called just-in-time when a member saves a LeetCode handle, so the rating
     * is present the first time they look at their profile rather than up to six
     * hours later.</p>
     *
     * <p>Not transactional: the save below is its own short transaction, and
     * wrapping the HTTP call in one would hold a pooled connection (of three) for
     * as long as LeetCode takes to answer.</p>
     *
     * @param user the member to refresh; null or handle-less members are skipped
     * @return true when LeetCode answered and the member was updated
     */
    public boolean syncSingleUser(User user) {
        if (user == null || user.getLeetcodeHandle() == null || user.getLeetcodeHandle().isBlank()) {
            return false;
        }
        if (fetchAndApply(user)) {
            persist(user);
            log.info("Synced LeetCode for '{}'", user.getLeetcodeHandle());
            return true;
        }
        return false;
    }

    /**
     * Refreshes every member who has linked a LeetCode account.
     *
     * <p>Called by the Codeforces cron once its own sync finishes, rather than
     * carrying a second {@code @Scheduled} annotation. Two independent schedules
     * would drift into overlapping, and both share one rate limiter â€” the second
     * job would spend its time blocked on the first.</p>
     *
     * <p>One member's failure does not stop the run: {@link #fetchAndApply} absorbs
     * it and returns false, and the loop continues. The run is recorded as
     * {@link SyncJob#LC_RATINGS} so it shows in Sync Health; it used to run with no
     * record at all, so a LeetCode outage was invisible.</p>
     *
     * <p>Not transactional, for the same reason as {@link #syncSingleUser}: one
     * transaction around the loop held a connection for the whole run, two seconds
     * per member.</p>
     */
    public void syncAllUsers() {
        SyncRunRecorder.RunHandle run = syncRunRecorder.start(SyncJob.LC_RATINGS);
        List<User> users = userRepository.findByLeetcodeHandleIsNotNull().stream()
                .filter(u -> u.getLeetcodeHandle() != null && !u.getLeetcodeHandle().isBlank())
                .toList();
        if (users.isEmpty()) {
            log.info("LeetCode sync: no members have linked a handle, nothing to do.");
            run.complete(0, 0, null);
            return;
        }

        log.info("Starting LeetCode rating synchronization for {} member(s)...", users.size());
        int updated = 0;
        String firstFailure = null;
        for (User user : users) {
            if (fetchAndApply(user)) {
                persist(user);
                updated++;
            } else if (firstFailure == null) {
                firstFailure = user.getLeetcodeHandle() + ": no usable response from LeetCode (check the username)";
            }
        }
        log.info("LeetCode rating synchronization finished. Updated {} of {} member(s).",
                updated, users.size());
        run.complete(updated, users.size() - updated, firstFailure);
    }

    /**
     * Writes only the LeetCode columns, and only while the member still has the
     * handle the data was fetched for. A plain save(user) wrote back every column
     * as it was when the run loaded the member -- reverting any profile edit made
     * in the meantime. See UserRepository's sync writes.
     */
    private void persist(User user) {
        userRepository.updateLeetcodeStats(user.getId(), user.getLeetcodeHandle(), user.getLeetcodeRating(),
                user.getLeetcodeTotalSolved(), user.getLeetcodeEasySolved(), user.getLeetcodeMediumSolved(),
                user.getLeetcodeHardSolved(), user.getLeetcodeSyncedAt());
    }

    @EventListener(condition = "#event.job.name() == 'LC_RATINGS'")
    public void handleManualTrigger(SyncTriggerEvent event) {
        syncAllUsers();
    }

    /**
     * Performs one rate-limited GraphQL call and applies the reply to {@code user}.
     *
     * @param user the member to refresh
     * @return true when the reply was usable and {@code user} was updated; false
     *         when nothing could be determined and the stored values are left alone
     */
    private boolean fetchAndApply(User user) {
        leetcodeRateLimiter.acquire();
        String handle = user.getLeetcodeHandle();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HttpHeaders.REFERER, "https://leetcode.com");
            headers.set(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT);

            Map<String, Object> body = Map.of(
                    "query", CONTEST_RANKING_QUERY,
                    "variables", Map.of("username", handle)
            );

            LeetCodeGraphQLResponse response = restTemplate.postForObject(
                    LEETCODE_GRAPHQL_URL,
                    new HttpEntity<>(body, headers),
                    LeetCodeGraphQLResponse.class
            );

            if (response == null || response.data() == null) {
                log.warn("LeetCode sync: empty response for handle '{}'.", handle);
                return false;
            }

            Double rating = response.extractRating();
            if (rating != null) {
                user.setLeetcodeRating((int) Math.round(rating));
            } else if (response.data().matchedUser() == null) {
                // No ranking AND no user: LeetCode's answer for a username that does not
                // exist. This used to be read as "never contested" and stored as a
                // successful sync with rating 0, hiding a mistyped username for good.
                log.warn("LeetCode sync: LeetCode has no user '{}'. The member should fix the username.", handle);
                return false;
            } else if (response.isUnratedMember()) {
                user.setLeetcodeRating(UNRATED);
            } else {
                log.warn("LeetCode sync: no contest data in response for handle '{}'.", handle);
                return false;
            }

            if (response.data().matchedUser() != null && response.data().matchedUser().submitStats() != null) {
                for (LeetCodeGraphQLResponse.AcSubmissionNum ac : response.data().matchedUser().submitStats().acSubmissionNum()) {
                    if (ac.count() == null) continue;
                    if ("All".equals(ac.difficulty())) user.setLeetcodeTotalSolved(ac.count());
                    else if ("Easy".equals(ac.difficulty())) user.setLeetcodeEasySolved(ac.count());
                    else if ("Medium".equals(ac.difficulty())) user.setLeetcodeMediumSolved(ac.count());
                    else if ("Hard".equals(ac.difficulty())) user.setLeetcodeHardSolved(ac.count());
                }
            }
            
            user.setLeetcodeSyncedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            return true;

        } catch (RestClientException e) {
            log.warn("LeetCode sync failed for handle '{}': {}", handle, e.getMessage());
            return false;
        } catch (RuntimeException e) {
            // A response shape LeetCode changed without notice surfaces here, not as a
            // RestClientException. The class contract is that nothing throws.
            log.warn("LeetCode sync could not read the reply for handle '{}': {}", handle, e.getMessage());
            return false;
        }
    }
}
