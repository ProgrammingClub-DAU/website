package com.cpclub.backend.leetcode;

import com.cpclub.backend.leetcode.dto.LeetCodeGraphQLResponse;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The LeetCode rating sync.
 *
 * <p>The service's contract is that it never throws: it runs inside a member's
 * own profile save, so a LeetCode outage must cost a rating refresh, never the
 * save. Most of these tests are about which responses write a value and which
 * leave the stored one alone.</p>
 */
@ExtendWith(MockitoExtension.class)
class LeetCodeSyncServiceTest {

    private static final String URL = "https://leetcode.com/graphql";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LeetCodeSyncService leetCodeSyncService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = User.builder().id(1L).name("Alice").leetcodeHandle("alice_lc").leetcodeRating(1400).build();

        // @InjectMocks bypasses Spring, so the limiter field stays null. An
        // unthrottled real limiter keeps the acquire() call on the tested path.
        ReflectionTestUtils.setField(
                leetCodeSyncService, "codeforcesRateLimiter", RateLimiter.create(Double.MAX_VALUE));
    }

    private static LeetCodeGraphQLResponse rated(double rating) {
        return new LeetCodeGraphQLResponse(new LeetCodeGraphQLResponse.Data(
                new LeetCodeGraphQLResponse.UserContestRanking(rating)));
    }

    private void respondWith(LeetCodeGraphQLResponse response) {
        when(restTemplate.postForObject(eq(URL), any(HttpEntity.class), eq(LeetCodeGraphQLResponse.class)))
                .thenReturn(response);
    }

    @Test
    @DisplayName("A fractional LeetCode rating is rounded and saved")
    void sync_success() {
        respondWith(rated(1523.45));

        leetCodeSyncService.syncSingleUser(alice);

        assertEquals(1523, alice.getLeetcodeRating());
        verify(userRepository).save(alice);
    }

    @Test
    @DisplayName("The request carries the handle and the Referer LeetCode requires")
    @SuppressWarnings("unchecked")
    void sync_sendsHandleAndReferer() {
        respondWith(rated(1600));
        ArgumentCaptor<HttpEntity<Map<String, Object>>> request = ArgumentCaptor.forClass(HttpEntity.class);

        leetCodeSyncService.syncSingleUser(alice);

        verify(restTemplate).postForObject(eq(URL), request.capture(), eq(LeetCodeGraphQLResponse.class));
        // Without a Referer, LeetCode answers 403 -- and the sync would silently
        // stop updating anyone, because failures are swallowed by design.
        assertEquals("https://leetcode.com", request.getValue().getHeaders().getFirst(HttpHeaders.REFERER));
        assertEquals(Map.of("username", "alice_lc"), request.getValue().getBody().get("variables"));
    }

    @Test
    @DisplayName("A member with no LeetCode handle makes no HTTP call")
    void sync_nullHandle() {
        alice.setLeetcodeHandle(null);

        leetCodeSyncService.syncSingleUser(alice);

        verifyNoInteractions(restTemplate);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("A blank handle is treated the same as no handle")
    void sync_blankHandle() {
        alice.setLeetcodeHandle("   ");

        leetCodeSyncService.syncSingleUser(alice);

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("A member who has never entered a contest is recorded as 0")
    void sync_noContest() {
        // LeetCode answers a valid handle with no contest history by returning
        // data with a null userContestRanking. That is a real answer, so it
        // writes 0 rather than leaving the old rating in place.
        respondWith(new LeetCodeGraphQLResponse(new LeetCodeGraphQLResponse.Data(null)));

        leetCodeSyncService.syncSingleUser(alice);

        assertEquals(0, alice.getLeetcodeRating());
        verify(userRepository).save(alice);
    }

    @Test
    @DisplayName("A response with no data leaves the stored rating alone")
    void sync_malformedResponse() {
        respondWith(new LeetCodeGraphQLResponse(null));

        leetCodeSyncService.syncSingleUser(alice);

        assertEquals(1400, alice.getLeetcodeRating());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("An HTTP failure is absorbed, and the rating is unchanged")
    void sync_httpError() {
        when(restTemplate.postForObject(eq(URL), any(HttpEntity.class), eq(LeetCodeGraphQLResponse.class)))
                .thenThrow(new RestClientException("503 Service Unavailable"));

        assertDoesNotThrow(() -> leetCodeSyncService.syncSingleUser(alice));

        assertEquals(1400, alice.getLeetcodeRating());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("The bulk sync carries on past one member's failure")
    void syncAll_continuesAfterAFailure() {
        User bob = User.builder().id(2L).name("Bob").leetcodeHandle("bob_lc").leetcodeRating(1500).build();
        when(userRepository.findByLeetcodeHandleIsNotNull()).thenReturn(List.of(alice, bob));
        when(restTemplate.postForObject(eq(URL), any(HttpEntity.class), eq(LeetCodeGraphQLResponse.class)))
                .thenThrow(new RestClientException("timeout"))
                .thenReturn(rated(1812.6));

        leetCodeSyncService.syncAllUsers();

        assertEquals(1400, alice.getLeetcodeRating(), "the failed member keeps their rating");
        assertEquals(1813, bob.getLeetcodeRating(), "the next member is still updated");
        verify(userRepository, never()).save(alice);
        verify(userRepository).save(bob);
    }
}
