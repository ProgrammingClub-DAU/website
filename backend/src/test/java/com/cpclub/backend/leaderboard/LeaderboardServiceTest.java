package com.cpclub.backend.leaderboard;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Test
    void getLeaderboard_calculatesRanksCorrectly_noTies() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        
        List<User> users = Arrays.asList(
            createUser(1L, 1800),
            createUser(2L, 1700),
            createUser(3L, 1600)
        );
        when(repository.findAllByOrderByRatingDescNullsLast(any())).thenReturn(new PageImpl<>(users, PageRequest.of(0, 10), 3));
        when(repository.countByRatingGreaterThan(1800)).thenReturn(0L);
        when(repository.countByRatingGreaterThan(1700)).thenReturn(1L);
        when(repository.countByRatingGreaterThan(1600)).thenReturn(2L);

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(0, 10);
        
        assertEquals(3, response.content().size());
        assertEquals(1, response.content().get(0).rank());
        assertEquals(2, response.content().get(1).rank());
        assertEquals(3, response.content().get(2).rank());
    }

    @Test
    void getLeaderboard_calculatesRanksCorrectly_withTies() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        
        List<User> users = Arrays.asList(
            createUser(1L, 1800),
            createUser(2L, 1800),
            createUser(3L, 1700)
        );
        when(repository.findAllByOrderByRatingDescNullsLast(any())).thenReturn(new PageImpl<>(users, PageRequest.of(0, 10), 3));
        when(repository.countByRatingGreaterThan(1800)).thenReturn(0L);
        when(repository.countByRatingGreaterThan(1700)).thenReturn(2L);

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(0, 10);
        
        assertEquals(3, response.content().size());
        assertEquals(1, response.content().get(0).rank());
        assertEquals(1, response.content().get(1).rank());
        assertEquals(3, response.content().get(2).rank());
    }

    @Test
    void getLeaderboard_calculatesRanksCorrectly_multipleTies() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        
        List<User> users = Arrays.asList(
            createUser(1L, 1800),
            createUser(2L, 1800),
            createUser(3L, 1700),
            createUser(4L, 1600),
            createUser(5L, 1600)
        );
        when(repository.findAllByOrderByRatingDescNullsLast(any())).thenReturn(new PageImpl<>(users, PageRequest.of(0, 10), 5));
        when(repository.countByRatingGreaterThan(1800)).thenReturn(0L);
        when(repository.countByRatingGreaterThan(1700)).thenReturn(2L);
        when(repository.countByRatingGreaterThan(1600)).thenReturn(3L);

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(0, 10);
        
        assertEquals(5, response.content().size());
        assertEquals(1, response.content().get(0).rank());
        assertEquals(1, response.content().get(1).rank());
        assertEquals(3, response.content().get(2).rank());
        assertEquals(4, response.content().get(3).rank());
        assertEquals(4, response.content().get(4).rank());
    }

    @Test
    void getLeaderboard_calculatesRanksCorrectly_tiesAcrossPages() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        
        // Simulating Page 2 where the users have 1700 rating, but there are already two 1800s and one 1700 on Page 1.
        List<User> users = Arrays.asList(
            createUser(4L, 1700),
            createUser(5L, 1600)
        );
        when(repository.findAllByOrderByRatingDescNullsLast(any())).thenReturn(new PageImpl<>(users, PageRequest.of(1, 2), 5));
        when(repository.countByRatingGreaterThan(1700)).thenReturn(2L); // the two 1800 users
        when(repository.countByRatingGreaterThan(1600)).thenReturn(4L); // the two 1800s + two 1700s

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(1, 2);
        
        assertEquals(2, response.content().size());
        assertEquals(3, response.content().get(0).rank());
        assertEquals(5, response.content().get(1).rank());
    }

    @Test
    void getLeaderboard_calculatesRanksCorrectly_nullRatings() {
        UserRepository repository = mock(UserRepository.class);
        LeaderboardService service = new LeaderboardService(repository);
        
        List<User> users = Arrays.asList(
            createUser(1L, 1500),
            createUser(2L, null),
            createUser(3L, null)
        );
        when(repository.findAllByOrderByRatingDescNullsLast(any())).thenReturn(new PageImpl<>(users, PageRequest.of(0, 10), 3));
        when(repository.countByRatingGreaterThan(1500)).thenReturn(0L);
        when(repository.countByRatingIsNotNull()).thenReturn(1L);

        PagedResponse<LeaderboardResponseDto> response = service.getLeaderboard(0, 10);
        
        assertEquals(3, response.content().size());
        assertEquals(1, response.content().get(0).rank());
        assertEquals(2, response.content().get(1).rank());
        assertEquals(2, response.content().get(2).rank());
    }

    private User createUser(Long id, Integer rating) {
        User user = new User();
        user.setId(id);
        user.setRating(rating);
        return user;
    }
}
