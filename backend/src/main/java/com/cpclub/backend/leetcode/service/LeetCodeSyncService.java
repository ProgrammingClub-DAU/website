package com.cpclub.backend.leetcode.service;

import com.cpclub.backend.leetcode.dto.LeetCodeGraphQLResponse;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Keeps members' LeetCode contest ratings up to date.
 *
 * <p>LeetCode publishes no REST API. The only public surface is the same GraphQL
 * endpoint the website itself uses, which is undocumented and unversioned — so
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
            }
            """;

    /** Recorded when a handle is valid but has never entered a rated contest. */
    private static final int UNRATED = 0;

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    /**
     * Shared outbound gate, injected from
     * {@link com.cpclub.backend.common.config.AppConfig}.
     *
     * <p>This is the Codeforces limiter, reused deliberately. It is the single
     * throttle on this application's outbound sync traffic, and both jobs run on
     * the same schedule — giving LeetCode its own limiter would let the two fire
     * concurrently and double the burst this instance produces.</p>
     */
    private final RateLimiter codeforcesRateLimiter;

    /**
     * Refreshes one member's rating, on demand.
     *
     * <p>Called just-in-time when a member saves a LeetCode handle, so the rating
     * is present the first time they look at their profile rather than up to six
     * hours later.</p>
     *
     * @param user the member to refresh; null or handle-less members are skipped
     */
    @Transactional
    public void syncSingleUser(User user) {
        if (user == null || user.getLeetcodeHandle() == null || user.getLeetcodeHandle().isBlank()) {
            return;
        }
        Integer rating = fetchRating(user.getLeetcodeHandle());
        if (rating == null) {
            return;
        }
        user.setLeetcodeRating(rating);
        userRepository.save(user);
        log.info("Synced LeetCode rating for '{}': {}", user.getLeetcodeHandle(), rating);
    }

    /**
     * Refreshes every member who has linked a LeetCode account.
     *
     * <p>Called by the Codeforces cron once its own sync finishes, rather than
     * carrying a second {@code @Scheduled} annotation. Two independent schedules
     * would drift into overlapping, and both share one rate limiter — the second
     * job would spend its time blocked on the first.</p>
     *
     * <p>One member's failure does not stop the run: {@link #fetchRating} absorbs
     * it and returns null, and the loop continues.</p>
     */
    @Transactional
    public void syncAllUsers() {
        List<User> users = userRepository.findByLeetcodeHandleIsNotNull();
        if (users.isEmpty()) {
            log.info("LeetCode sync: no members have linked a handle, nothing to do.");
            return;
        }

        log.info("Starting LeetCode rating synchronization for {} member(s)...", users.size());
        int updated = 0;
        for (User user : users) {
            Integer rating = fetchRating(user.getLeetcodeHandle());
            if (rating == null) {
                continue;
            }
            user.setLeetcodeRating(rating);
            userRepository.save(user);
            updated++;
        }
        log.info("LeetCode rating synchronization finished. Updated {} of {} member(s).",
                updated, users.size());
    }

    /**
     * Performs one rate-limited GraphQL call and interprets the reply.
     *
     * @param handle the member's LeetCode username
     * @return the rating to store, {@link #UNRATED} for a member who has never
     *         contested, or null when nothing could be determined and the stored
     *         value should be left alone
     */
    private Integer fetchRating(String handle) {
        // Blocks until a permit is free. Shared with the Codeforces sync, so the
        // two jobs cannot combine into a burst against either provider.
        codeforcesRateLimiter.acquire();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // LeetCode returns 403 to requests without a browser-ish Referer.
            headers.set(HttpHeaders.REFERER, "https://leetcode.com");

            Map<String, Object> body = Map.of(
                    "query", CONTEST_RANKING_QUERY,
                    "variables", Map.of("username", handle)
            );

            LeetCodeGraphQLResponse response = restTemplate.postForObject(
                    LEETCODE_GRAPHQL_URL,
                    new HttpEntity<>(body, headers),
                    LeetCodeGraphQLResponse.class
            );

            if (response == null) {
                log.warn("LeetCode sync: empty response for handle '{}'.", handle);
                return null;
            }

            Double rating = response.extractRating();
            if (rating != null) {
                return (int) Math.round(rating);
            }

            if (response.isUnratedMember()) {
                // A real answer, not a failure: the handle exists and has never
                // been in a rated contest. Recording zero distinguishes that from
                // "we do not know", which is what leaving the field null means.
                return UNRATED;
            }

            log.warn("LeetCode sync: no contest data in response for handle '{}'.", handle);
            return null;

        } catch (RestClientException e) {
            // Deliberately swallowed. See the class javadoc: this runs inside a
            // member's own profile save, and a LeetCode outage must not stop it.
            log.warn("LeetCode sync failed for handle '{}': {}", handle, e.getMessage());
            return null;
        }
    }
}
