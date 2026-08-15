package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.dto.CodeforcesResponse;
import com.cpclub.backend.codeforces.dto.CodeforcesUserDto;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CodeforcesSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CodeforcesSyncService codeforcesSyncService;

    private static final String API = "https://codeforces.com/api/user.info?handles=";

    private User tourist;
    private User badHandle;

    @BeforeEach
    void setUp() {
        tourist = User.builder().id(1L).codeforcesHandle("tourist").rating(3000).build();
        badHandle = User.builder().id(2L).codeforcesHandle("not_a_real_handle").rating(1500).build();

        // @InjectMocks bypasses Spring DI, so the RateLimiter field stays null.
        // Inject a max-throughput limiter so tests don't throttle themselves
        // while still exercising the real acquire() code path.
        ReflectionTestUtils.setField(
                codeforcesSyncService, "codeforcesRateLimiter", RateLimiter.create(Double.MAX_VALUE));
    }

    private static CodeforcesResponse ok(CodeforcesUserDto... users) {
        return new CodeforcesResponse("OK", null, List.of(users));
    }

    private static CodeforcesUserDto cfUser(String handle, Integer rating) {
        return new CodeforcesUserDto(handle, rating, rating, "expert", "expert");
    }

    @Test
    @DisplayName("Updates ratings when the provider returns OK")
    void syncCodeforcesRatings_Success() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class)))
                .thenReturn(ok(cfUser("tourist", 4000)));

        codeforcesSyncService.syncCodeforcesRatings();

        verify(userRepository, times(1)).save(tourist);
        assertEquals(4000, tourist.getRating());
    }

    /**
     * The regression this class most needed. The Codeforces user.info endpoint is
     * all-or-nothing: one invalid handle fails the whole batch. Before the
     * per-handle fallback, that meant a single member's typo silently froze every
     * member's rating indefinitely.
     */
    @Test
    @DisplayName("One bad handle does not block the rest of the club")
    void syncCodeforcesRatings_BadHandleDoesNotBlockOthers() {
        when(userRepository.findByCodeforcesHandleIsNotNull())
                .thenReturn(List.of(tourist, badHandle));

        // Exact URLs, not `contains` — "handles=tourist" is also a substring of the
        // combined batch URL, which makes the stubs ambiguous.
        when(restTemplate.getForObject(eq(API + "tourist;not_a_real_handle"), eq(CodeforcesResponse.class)))
                .thenReturn(new CodeforcesResponse("FAILED",
                        "handles: User with handle not_a_real_handle not found", null));
        // Retried individually: the good handle succeeds, the bad one still fails.
        when(restTemplate.getForObject(eq(API + "tourist"), eq(CodeforcesResponse.class)))
                .thenReturn(ok(cfUser("tourist", 3900)));
        when(restTemplate.getForObject(eq(API + "not_a_real_handle"), eq(CodeforcesResponse.class)))
                .thenReturn(new CodeforcesResponse("FAILED", "not found", null));

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3900, tourist.getRating(), "valid handle must still update");
        assertEquals(1500, badHandle.getRating(), "invalid handle keeps its previous rating");
        verify(userRepository, times(1)).save(tourist);
        verify(userRepository, never()).save(badHandle);
    }

    @Test
    @DisplayName("A provider outage leaves existing ratings intact")
    void syncCodeforcesRatings_ProviderThrows() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class)))
                .thenThrow(new RestClientException("connection reset"));

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3000, tourist.getRating(), "must not wipe the last known rating");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("A null response body is handled without throwing")
    void syncCodeforcesRatings_NullResponse() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class))).thenReturn(null);

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3000, tourist.getRating());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Unrated accounts are skipped rather than written as zero")
    void syncCodeforcesRatings_UnratedUserIsSkipped() {
        User unrated = User.builder().id(3L).codeforcesHandle("fresh_account").rating(null).build();
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(unrated));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class)))
                .thenReturn(ok(cfUser("fresh_account", null)));

        codeforcesSyncService.syncCodeforcesRatings();

        assertNull(unrated.getRating());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Handles are matched case-insensitively, as Codeforces treats them")
    void syncCodeforcesRatings_MatchesHandleCaseInsensitively() {
        User mixedCase = User.builder().id(4L).codeforcesHandle("ToUrIsT").rating(1000).build();
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(mixedCase));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class)))
                .thenReturn(ok(cfUser("tourist", 3800)));

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3800, mixedCase.getRating());
        verify(userRepository, times(1)).save(mixedCase);
    }

    @Test
    @DisplayName("No handles means no outbound request at all")
    void syncCodeforcesRatings_NoHandlesSkipsRequest() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of());

        codeforcesSyncService.syncCodeforcesRatings();

        verifyNoInteractions(restTemplate);
    }
}
