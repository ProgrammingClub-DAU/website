package com.cpclub.backend.user.service;

import com.cpclub.backend.codeforces.service.CodeforcesSubmissionSyncService;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.leetcode.service.LeetCodeTotalsSnapshotService;
import com.cpclub.backend.score.service.MemberScoreService;
import com.cpclub.backend.stats.service.ContestHistorySyncService;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The startup pickup of members whose handles have never synced. */
class MemberStatsSyncServiceTest {

    private UserRepository users;
    private MemberStatsSyncService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        service = new MemberStatsSyncService(users,
                mock(CodeforcesSyncService.class),
                mock(CodeforcesSubmissionSyncService.class),
                mock(ContestHistorySyncService.class),
                mock(LeetCodeSyncService.class),
                mock(LeetCodeTotalsSnapshotService.class),
                mock(MemberScoreService.class),
                Clock.systemUTC());
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    @DisplayName("Queues every member whose handle has not synced yet")
    void queuesMembersAwaitingFirstSync() {
        when(users.findIdsAwaitingFirstSync()).thenReturn(List.of(7L, 8L));
        when(users.findById(anyLong())).thenReturn(Optional.empty());

        service.queueMembersAwaitingFirstSync();

        // The refresh runs on the worker and starts by loading the member.
        verify(users, timeout(2000)).findById(7L);
        verify(users, timeout(2000)).findById(8L);
    }

    @Test
    @DisplayName("A failed lookup is logged, not thrown, so startup carries on")
    void lookupFailureDoesNotStopStartup() {
        when(users.findIdsAwaitingFirstSync()).thenThrow(new IllegalStateException("database unavailable"));

        assertDoesNotThrow(service::queueMembersAwaitingFirstSync);
    }
}
