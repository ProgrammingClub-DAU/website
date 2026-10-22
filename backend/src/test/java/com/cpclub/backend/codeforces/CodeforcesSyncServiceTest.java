package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.dto.CodeforcesResponse;
import com.cpclub.backend.codeforces.dto.CodeforcesUserDto;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.codeforces.service.CodeforcesSubmissionSyncService;
import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.stats.service.ContestHistorySyncService;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.service.SyncJobGuard;
import com.cpclub.backend.sync.service.SyncRunRecorder;
import com.cpclub.backend.user.repository.UserRepository;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CodeforcesSyncServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestTemplate restTemplate;

    /**
     * Chained off the end of the scheduled sync. Without a mock here the field
     * is null under @InjectMocks and every test in this class fails on the call.
     */
    @Mock
    private LeetCodeSyncService leetCodeSyncService;

    @Mock
    private CodeforcesSubmissionSyncService codeforcesSubmissionSyncService;

    @Mock
    private ContestHistorySyncService contestHistorySyncService;

    @Mock
    private MemberScoreService memberScoreService;

    @Mock
    private SyncRunRecorder syncRunRecorder;

    @Spy
    private SyncJobGuard syncJobGuard = new SyncJobGuard();

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
        lenient().when(syncRunRecorder.start(any())).thenReturn(SyncRunRecorder.RunHandle.detached());
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

        verify(userRepository).updateCodeforcesRating(1L, "tourist", 4000, 4000);
        assertEquals(4000, tourist.getRating());
        assertEquals(4000, tourist.getMaxRating(), "peak rating is stored too");
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
        // Codeforces names the missing handle, so it is dropped and the rest retried
        // in one request -- no need to probe each handle on its own.
        when(restTemplate.getForObject(eq(API + "tourist"), eq(CodeforcesResponse.class)))
                .thenReturn(ok(cfUser("tourist", 3900)));

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3900, tourist.getRating(), "valid handle must still update");
        assertEquals(1500, badHandle.getRating(), "invalid handle keeps its previous rating");
        verify(userRepository).updateCodeforcesRating(eq(1L), eq("tourist"), eq(3900), any());
        verify(userRepository, never()).updateCodeforcesRating(eq(2L), any(), any(), any());
        verify(restTemplate, never()).getForObject(eq(API + "not_a_real_handle"), eq(CodeforcesResponse.class));
    }

    @Test
    @DisplayName("A provider outage leaves existing ratings intact")
    void syncCodeforcesRatings_ProviderThrows() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class)))
                .thenThrow(new RestClientException("connection reset"));

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3000, tourist.getRating(), "must not wipe the last known rating");
        verify(userRepository, never()).updateCodeforcesRating(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("A null response body is handled without throwing")
    void syncCodeforcesRatings_NullResponse() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist));
        when(restTemplate.getForObject(anyString(), eq(CodeforcesResponse.class))).thenReturn(null);

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3000, tourist.getRating());
        verify(userRepository, never()).updateCodeforcesRating(anyLong(), any(), any(), any());
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
        verify(userRepository, never()).updateCodeforcesRating(anyLong(), any(), any(), any());
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
        verify(userRepository).updateCodeforcesRating(4L, "ToUrIsT", 3800, 3800);
    }

    @Test
    @DisplayName("No handles means no outbound request at all")
    void syncCodeforcesRatings_NoHandlesSkipsRequest() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of());

        codeforcesSyncService.syncCodeforcesRatings();

        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("The LeetCode bulk sync is chained off the end of the Codeforces run")
    void syncCodeforcesRatings_ChainsLeetCodeSync() {
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist));
        when(restTemplate.getForObject(API + "tourist", CodeforcesResponse.class))
                .thenReturn(ok(cfUser("tourist", 3100)));

        codeforcesSyncService.syncCodeforcesRatings();

        verify(leetCodeSyncService).syncAllUsers();
    }

    /**
     * What Codeforces really does, as seen in production: an unknown handle comes
     * back as HTTP 400 with the JSON envelope in the body, raised by RestTemplate as
     * an exception. The body used to be discarded, so every bad handle was found by
     * bisecting (about eight extra requests each) and recorded as "no response".
     */
    @Test
    @DisplayName("A 400 naming the missing handle drops it and retries once, and the run lists it")
    void syncCodeforcesRatings_http400NamingHandle_retriesOnceAndRecordsIt() {
        com.cpclub.backend.sync.repository.SyncRunRepository runRepo =
                mock(com.cpclub.backend.sync.repository.SyncRunRepository.class);
        when(runRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        // Built before when(): calling another mock inside thenReturn(...) confuses the stubbing.
        SyncRunRecorder.RunHandle realRun = new SyncRunRecorder(runRepo).start(SyncJob.CF_RATINGS);
        when(syncRunRecorder.start(SyncJob.CF_RATINGS)).thenReturn(realRun);

        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of(tourist, badHandle));
        when(restTemplate.getForObject(eq(API + "tourist;not_a_real_handle"), eq(CodeforcesResponse.class)))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Bad Request",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"status\":\"FAILED\",\"comment\":\"handles: User with handle not_a_real_handle not found\"}"
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        java.nio.charset.StandardCharsets.UTF_8));
        when(restTemplate.getForObject(eq(API + "tourist"), eq(CodeforcesResponse.class)))
                .thenReturn(ok(cfUser("tourist", 3900)));

        codeforcesSyncService.syncCodeforcesRatings();

        assertEquals(3900, tourist.getRating());
        verify(restTemplate, times(2)).getForObject(anyString(), eq(CodeforcesResponse.class));

        org.mockito.ArgumentCaptor<com.cpclub.backend.sync.entity.SyncRun> saved =
                org.mockito.ArgumentCaptor.forClass(com.cpclub.backend.sync.entity.SyncRun.class);
        verify(runRepo, atLeastOnce()).save(saved.capture());
        com.cpclub.backend.sync.entity.SyncRun finished = saved.getValue();
        assertEquals(1, finished.getMembersOk());
        assertEquals(1, finished.getMembersFailed());
        org.junit.jupiter.api.Assertions.assertTrue(
                finished.getErrorSample().contains("Not found on Codeforces")
                        && finished.getErrorSample().contains("not_a_real_handle"),
                finished.getErrorSample());
    }

    @Test
    @DisplayName("A step already running by hand is skipped by the scheduled pipeline, not run twice")
    void syncCodeforcesRatings_skipsStepAlreadyRunning() {
        syncJobGuard.tryStart(SyncJob.CF_SUBMISSIONS);
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of());

        codeforcesSyncService.syncCodeforcesRatings();

        verify(codeforcesSubmissionSyncService, never()).syncAll();
        verify(leetCodeSyncService).syncAllUsers();
    }

    @Test
    @DisplayName("LeetCode still syncs when no member has linked a Codeforces handle")
    void syncCodeforcesRatings_ChainsLeetCodeSyncEvenWhenNoCodeforcesHandles() {
        // The Codeforces half returns early here. Before the method was split,
        // that early return skipped the LeetCode sync entirely — a club where
        // nobody used Codeforces would never have had a LeetCode rating refreshed.
        when(userRepository.findByCodeforcesHandleIsNotNull()).thenReturn(List.of());

        codeforcesSyncService.syncCodeforcesRatings();

        verify(restTemplate, never()).getForObject(anyString(), eq(CodeforcesResponse.class));
        verify(leetCodeSyncService).syncAllUsers();
    }
}
