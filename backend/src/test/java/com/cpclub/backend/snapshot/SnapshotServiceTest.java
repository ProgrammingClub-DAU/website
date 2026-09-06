package com.cpclub.backend.snapshot;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.snapshot.dto.RatingPointDto;
import com.cpclub.backend.snapshot.entity.WeeklySnapshot;
import com.cpclub.backend.snapshot.repository.WeeklySnapshotRepository;
import com.cpclub.backend.snapshot.service.SnapshotService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the weekly snapshot job and the rating history reads.
 */
class SnapshotServiceTest {

    private WeeklySnapshotRepository snapshotRepository;
    private UserRepository userRepository;
    private SnapshotService service;

    @BeforeEach
    void setUp() {
        snapshotRepository = mock(WeeklySnapshotRepository.class);
        userRepository = mock(UserRepository.class);
        service = new SnapshotService(snapshotRepository, userRepository);
    }

    private User user(Long id, Integer cfRating, Integer lcRating) {
        return User.builder()
                .id(id)
                .name("Member " + id)
                .email("member" + id + "@dau.ac.in")
                .password("hash")
                .rating(cfRating)
                .leetcodeRating(lcRating)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<WeeklySnapshot> captureSaved() {
        ArgumentCaptor<List<WeeklySnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(snapshotRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("A member rated on both platforms produces one row per platform")
    void recordWeeklySnapshots_writesOneRowPerRatedPlatform() {
        when(snapshotRepository.existsByRecordedAtGreaterThanEqual(any())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(List.of(user(1L, 1500, 1800)));

        service.recordWeeklySnapshots();

        List<WeeklySnapshot> saved = captureSaved();
        assertEquals(2, saved.size());
        assertTrue(saved.stream().anyMatch(s ->
                WeeklySnapshot.PLATFORM_CODEFORCES.equals(s.getPlatform()) && s.getRating() == 1500));
        assertTrue(saved.stream().anyMatch(s ->
                WeeklySnapshot.PLATFORM_LEETCODE.equals(s.getPlatform()) && s.getRating() == 1800));
    }

    @Test
    @DisplayName("A platform with no rating contributes no row, and does not block the other")
    void recordWeeklySnapshots_skipsUnratedPlatforms() {
        when(snapshotRepository.existsByRecordedAtGreaterThanEqual(any())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(List.of(user(1L, 1500, null)));

        service.recordWeeklySnapshots();

        List<WeeklySnapshot> saved = captureSaved();
        assertEquals(1, saved.size());
        assertEquals(WeeklySnapshot.PLATFORM_CODEFORCES, saved.get(0).getPlatform());
    }

    @Test
    @DisplayName("A second run on the same day writes nothing")
    void recordWeeklySnapshots_isIdempotentWithinADay() {
        when(snapshotRepository.existsByRecordedAtGreaterThanEqual(any())).thenReturn(true);

        service.recordWeeklySnapshots();

        verify(snapshotRepository, never()).saveAll(any());
        verify(userRepository, never()).findAll();
    }

    @Test
    @DisplayName("No rated members means no write at all, not an empty batch")
    void recordWeeklySnapshots_writesNothingWhenNobodyIsRated() {
        when(snapshotRepository.existsByRecordedAtGreaterThanEqual(any())).thenReturn(false);
        when(userRepository.findAll()).thenReturn(List.of(user(1L, null, null)));

        service.recordWeeklySnapshots();

        verify(snapshotRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("History maps snapshots to chart points, preserving repository order")
    void getCodeforcesHistory_mapsToChartPoints() {
        User member = user(1L, 1500, null);
        WeeklySnapshot older = WeeklySnapshot.builder()
                .user(member).platform(WeeklySnapshot.PLATFORM_CODEFORCES).rating(1400)
                .recordedAt(LocalDateTime.of(2026, 1, 5, 0, 0)).build();
        WeeklySnapshot newer = WeeklySnapshot.builder()
                .user(member).platform(WeeklySnapshot.PLATFORM_CODEFORCES).rating(1500)
                .recordedAt(LocalDateTime.of(2026, 1, 12, 0, 0)).build();

        when(userRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findByUserIdAndPlatformOrderByRecordedAtAsc(
                1L, WeeklySnapshot.PLATFORM_CODEFORCES)).thenReturn(List.of(older, newer));

        List<RatingPointDto> history = service.getCodeforcesHistory(1L);

        assertEquals(2, history.size());
        assertEquals(1400, history.get(0).rating());
        assertEquals(2026, history.get(0).date().getYear());
        assertEquals(5, history.get(0).date().getDayOfMonth());
        assertEquals(1500, history.get(1).rating());
    }

    @Test
    @DisplayName("A member with no snapshots yet gets an empty chart, not a 404")
    void getLeetcodeHistory_returnsEmptyListWhenNoSnapshots() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(snapshotRepository.findByUserIdAndPlatformOrderByRecordedAtAsc(
                1L, WeeklySnapshot.PLATFORM_LEETCODE)).thenReturn(List.of());

        assertTrue(service.getLeetcodeHistory(1L).isEmpty());
    }

    @Test
    @DisplayName("An unknown member id is a 404, so a typo is distinguishable from a new account")
    void getHistory_throwsWhenMemberDoesNotExist() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.getCodeforcesHistory(99L));
        assertThrows(ResourceNotFoundException.class, () -> service.getLeetcodeHistory(99L));
        verify(snapshotRepository, never())
                .findByUserIdAndPlatformOrderByRecordedAtAsc(anyLong(), eq(WeeklySnapshot.PLATFORM_CODEFORCES));
    }
}
