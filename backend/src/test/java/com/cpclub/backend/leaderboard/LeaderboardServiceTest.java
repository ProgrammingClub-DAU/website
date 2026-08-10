package com.cpclub.backend.leaderboard;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardServiceTest {

    @Test
    void calculateTier_coversEveryRatingBoundary() {
        Map<Integer, String> tiers = new LinkedHashMap<>();
        tiers.put(0, "Newbie");
        tiers.put(1199, "Newbie");
        tiers.put(1200, "Pupil");
        tiers.put(1400, "Specialist");
        tiers.put(1600, "Expert");
        tiers.put(1900, "Candidate Master");
        tiers.put(2100, "Master");
        tiers.put(2300, "International Master");
        tiers.put(2400, "International Grandmaster");
        tiers.put(2600, "Grandmaster");
        tiers.put(3000, "Legendary Grandmaster");

        assertEquals("Unrated", LeaderboardResponseDto.calculateTier(null));
        tiers.forEach((rating, expectedTier) -> assertEquals(expectedTier, LeaderboardResponseDto.calculateTier(rating)));
    }

    @Test
    void getLeaderboard_returnsEmptyPageWhenNoMembersExist() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        when(repository.findAllByOrderByRatingDescNullsLast(any()))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(0, 20);

        assertTrue(response.content().isEmpty());
        assertEquals(0, response.totalElements());
        assertTrue(response.last());
    }
}
