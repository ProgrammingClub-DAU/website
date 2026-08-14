package com.cpclub.backend.leaderboard;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardEntryProjection;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the parts of the leaderboard that live in Java: tier thresholds
 * and projection-to-DTO mapping.
 *
 * <p>Rank <em>calculation</em> is deliberately not tested here. It is performed by
 * a SQL window function, so asserting it against a mocked repository would only
 * prove the mock returns what it was told to. Those assertions live in
 * {@link LeaderboardRankingIntegrationTest}, which runs the real query against a
 * real database.</p>
 */
class LeaderboardServiceTest {

    @Test
    void calculateTier_coversEveryRatingBoundary() {
        Map<Integer, String> tiers = new LinkedHashMap<>();
        tiers.put(0, "Newbie");
        tiers.put(1199, "Newbie");
        tiers.put(1200, "Pupil");
        tiers.put(1399, "Pupil");
        tiers.put(1400, "Specialist");
        tiers.put(1599, "Specialist");
        tiers.put(1600, "Expert");
        tiers.put(1899, "Expert");
        tiers.put(1900, "Candidate Master");
        tiers.put(2099, "Candidate Master");
        tiers.put(2100, "Master");
        tiers.put(2299, "Master");
        tiers.put(2300, "International Master");
        tiers.put(2399, "International Master");
        tiers.put(2400, "International Grandmaster");
        tiers.put(2599, "International Grandmaster");
        tiers.put(2600, "Grandmaster");
        tiers.put(2999, "Grandmaster");
        tiers.put(3000, "Legendary Grandmaster");
        tiers.put(4000, "Legendary Grandmaster");

        assertEquals("Unrated", LeaderboardResponseDto.calculateTier(null));
        tiers.forEach((rating, expectedTier) -> assertEquals(expectedTier, LeaderboardResponseDto.calculateTier(rating)));
    }

    @Test
    void getLeaderboard_returnsEmptyPageWhenNoMembersExist() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        when(repository.findLeaderboardPage(any()))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(0, 20);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertTrue(response.last());
    }

    @Test
    @DisplayName("Every projection field reaches the response, and the tier is derived from the rating")
    void getLeaderboard_mapsProjectionOntoResponse() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        when(repository.findLeaderboardPage(any())).thenReturn(new PageImpl<>(
                List.of(row(7L, "Ada", "ada_cf", 1650, 4L)),
                PageRequest.of(0, 20), 1));

        LeaderboardResponseDto entry = service.getLeaderboard(0, 20).content().get(0);

        assertEquals(4, entry.rank());
        assertEquals(7L, entry.userId());
        assertEquals("Ada", entry.name());
        assertEquals("ada_cf", entry.codeforcesHandle());
        assertEquals(1650, entry.rating());
        assertEquals("Expert", entry.tier());
    }

    @Test
    @DisplayName("A member with no linked handle maps to nulls, not to a crash")
    void getLeaderboard_toleratesUnratedMemberWithoutHandle() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        when(repository.findLeaderboardPage(any())).thenReturn(new PageImpl<>(
                List.of(row(9L, "Newcomer", null, null, 12L)),
                PageRequest.of(0, 20), 1));

        LeaderboardResponseDto entry = service.getLeaderboard(0, 20).content().get(0);

        assertEquals(12, entry.rank());
        assertNull(entry.codeforcesHandle());
        assertNull(entry.rating());
        assertEquals("Unrated", entry.tier());
    }

    /** Builds a stub projection; Spring supplies the real implementation at runtime. */
    private LeaderboardEntryProjection row(Long id, String name, String handle, Integer rating, Long placement) {
        return new LeaderboardEntryProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getHandle() {
                return handle;
            }

            @Override
            public Integer getRating() {
                return rating;
            }

            @Override
            public Long getPlacement() {
                return placement;
            }
        };
    }
}
