package com.cpclub.backend.leaderboard;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Executes the real ranking query against a real database.
 *
 * <p>Ranking moved out of Java and into a SQL window function to remove an N+1
 * query — the previous implementation ran one {@code COUNT} per row, so a default
 * page of 20 members cost 22 round trips. Correctness therefore now depends on
 * the SQL itself, which a mocked repository cannot check.</p>
 *
 * <p>These cases mirror the scenarios the old unit tests covered against mocks,
 * so the standard competition ranking behaviour is provably unchanged: ties share
 * a position, the following rating skips the gap, and unrated members land last.</p>
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class LeaderboardRankingIntegrationTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    @DisplayName("Distinct ratings rank 1, 2, 3 in descending order")
    void ranksDistinctRatingsInOrder() {
        persist(1800, 1700, 1600);

        List<LeaderboardResponseDto> entries = page(0, 10);

        assertEquals(List.of(1, 2, 3), ranks(entries));
        assertEquals(List.of(1800, 1700, 1600), entries.stream().map(LeaderboardResponseDto::rating).toList());
    }

    @Test
    @DisplayName("Tied ratings share a rank and the next rating skips the gap")
    void tiedRatingsShareRankAndSkipTheGap() {
        persist(1800, 1800, 1700);

        assertEquals(List.of(1, 1, 3), ranks(page(0, 10)));
    }

    @Test
    @DisplayName("Several separate ties each skip their own gap")
    void multipleTieGroupsEachSkipTheirGap() {
        persist(1800, 1800, 1700, 1600, 1600);

        assertEquals(List.of(1, 1, 3, 4, 4), ranks(page(0, 10)));
    }

    @Test
    @DisplayName("Ranks stay absolute on page two rather than restarting at 1")
    void ranksAreAbsoluteAcrossPages() {
        persist(1800, 1800, 1700, 1600, 1500);

        // Page 0 holds the two tied 1800s; page 1 must continue from rank 3.
        assertEquals(List.of(1, 1), ranks(page(0, 2)));
        assertEquals(List.of(3, 4), ranks(page(1, 2)));
        assertEquals(List.of(5), ranks(page(2, 2)));
    }

    @Test
    @DisplayName("Unrated members sort last and tie with each other on one rank")
    void unratedMembersTieOnASingleTrailingRank() {
        persist(1500, null, null);

        List<LeaderboardResponseDto> entries = page(0, 10);

        assertEquals(List.of(1, 2, 2), ranks(entries));
        assertEquals("Unrated", entries.get(2).tier());
    }

    @Test
    @DisplayName("Total element count reflects the whole table, not the page")
    void pagingMetadataDescribesTheFullLeaderboard() {
        persist(1800, 1700, 1600, 1500, 1400);

        PagedResponse<LeaderboardResponseDto> response = leaderboardService.getLeaderboard(0, 2);

        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertEquals(2, response.content().size());
    }

    @Test
    @DisplayName("Query count does not grow with page size — the N+1 is gone")
    void pageCostsTheSameNumberOfQueriesRegardlessOfPageSize() {
        persist(1800, 1700, 1600, 1500, 1400, 1300, 1200, 1100, 1000, 900,
                800, 700, 600, 500, 400, 350, 300, 250, 200, 150);

        // The old implementation ran one COUNT per returned row, so cost scaled
        // with page size, not with table size: 5 queries for a page of 3 against
        // 22 for a page of 20. Both pages are full here, so both take the same
        // path through Spring Data's page-count optimization.
        long queriesForSmallPage = countQueriesForOnePage(3);
        long queriesForFullPage = countQueriesForOnePage(20);

        assertEquals(queriesForSmallPage, queriesForFullPage,
                "Rendering more rows must not cost more queries");
        assertEquals(2, queriesForFullPage,
                "One page query plus one count query, and nothing per row");
    }

    /** Executes one leaderboard page and reports how many statements it took. */
    private long countQueriesForOnePage(int size) {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        leaderboardService.getLeaderboard(0, size);
        return statistics.getQueryExecutionCount();
    }

    /** Counter kept across calls so a second persist() cannot collide on the unique email. */
    private int persisted = 0;

    private void persist(Integer... ratings) {
        for (Integer rating : ratings) {
            int n = persisted++;
            User user = new User("Member " + n, "member" + n + "@example.com", "hashed", Role.ROLE_USER);
            user.setCodeforcesHandle("handle" + n);
            user.setRating(rating);
            userRepository.save(user);
        }
        userRepository.flush();
    }

    private List<LeaderboardResponseDto> page(int page, int size) {
        return leaderboardService.getLeaderboard(page, size).content();
    }

    private List<Integer> ranks(List<LeaderboardResponseDto> entries) {
        return entries.stream().map(LeaderboardResponseDto::rank).toList();
    }
}
