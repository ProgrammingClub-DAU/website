package com.cpclub.backend.leaderboard;

import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import com.cpclub.backend.leaderboard.dto.LeaderboardPlatform;
import com.cpclub.backend.leaderboard.dto.LeaderboardResponseDto;
import com.cpclub.backend.leaderboard.service.LeaderboardService;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The platform and club-role filters, against a real database.
 *
 * <p>These run the query rather than mocking it on purpose. It is native SQL with
 * a window function, a CASE that switches the ranked column, and a WHERE clause
 * built from bound parameters — none of which a mocked repository would exercise,
 * and all of which can differ between H2 and PostgreSQL.</p>
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class LeaderboardFilterIntegrationTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("ALL returns every member regardless of club position")
    void allFilterIncludesEveryone() {
        persist("core", ClubRole.CORE, 1800, 1200);
        persist("rep", ClubRole.BATCH_REPRESENTATIVE, 1700, 1100);
        persist("plain", null, 1600, 1000);

        assertEquals(3, board(LeaderboardPlatform.CODEFORCES, LeaderboardFilter.ALL).size());
    }

    @Test
    @DisplayName("CORE covers all four leadership positions and nothing else")
    void coreFilterCoversTheWholeLeadership() {
        persist("convenor", ClubRole.CONVENOR, 1900, null);
        persist("deputy", ClubRole.DEPUTY_CONVENOR, 1850, null);
        persist("core", ClubRole.CORE, 1800, null);
        persist("associate", ClubRole.ASSOCIATE_CORE, 1750, null);
        persist("rep", ClubRole.BATCH_REPRESENTATIVE, 1700, null);
        persist("student", ClubRole.STUDENT, 1650, null);

        List<LeaderboardResponseDto> entries = board(LeaderboardPlatform.CODEFORCES, LeaderboardFilter.CORE);

        assertEquals(4, entries.size());
        assertTrue(entries.stream().map(LeaderboardResponseDto::clubRole).toList().containsAll(
                List.of("CONVENOR", "DEPUTY_CONVENOR", "CORE", "ASSOCIATE_CORE")));
    }

    @Test
    @DisplayName("STUDENTS includes members with no club position recorded")
    void studentsFilterIncludesUnassignedMembers() {
        // This is the case an IN list cannot express: every account created before
        // Phase 2 has a null club_role, and IN never matches NULL. Without the
        // explicit IS NULL branch the board would be empty on the day it shipped.
        persist("student", ClubRole.STUDENT, 1500, null);
        persist("unassigned", null, 1400, null);
        persist("core", ClubRole.CORE, 1800, null);

        List<LeaderboardResponseDto> entries = board(LeaderboardPlatform.CODEFORCES, LeaderboardFilter.STUDENTS);

        assertEquals(2, entries.size());
        assertEquals(List.of(1500, 1400), entries.stream().map(LeaderboardResponseDto::rating).toList());
    }

    @Test
    @DisplayName("Ranks are computed within the filtered board, not inherited from the full one")
    void ranksRestartWithinTheFilteredBoard() {
        persist("top", ClubRole.STUDENT, 2400, null);
        persist("second", ClubRole.STUDENT, 2300, null);
        persist("core", ClubRole.CORE, 1200, null);

        List<LeaderboardResponseDto> entries = board(LeaderboardPlatform.CODEFORCES, LeaderboardFilter.CORE);

        assertEquals(1, entries.size());
        assertEquals(1, entries.get(0).rank(),
                "the only member of a filtered board is first in it");
    }

    @Test
    @DisplayName("LEETCODE ranks by the LeetCode rating, which reorders the board")
    void leetcodePlatformRanksByTheOtherRating() {
        // Deliberately inverted: strongest on Codeforces is weakest on LeetCode.
        persist("alpha", ClubRole.STUDENT, 2000, 1000);
        persist("beta", ClubRole.STUDENT, 1000, 2000);

        // Asserting the ratings alone would prove nothing: both boards read
        // 2000 then 1000. The names are what has to swap.
        List<String> byCodeforces = board(LeaderboardPlatform.CODEFORCES, LeaderboardFilter.ALL)
                .stream().map(LeaderboardResponseDto::name).toList();
        List<String> byLeetcode = board(LeaderboardPlatform.LEETCODE, LeaderboardFilter.ALL)
                .stream().map(LeaderboardResponseDto::name).toList();

        assertEquals(List.of("alpha0", "beta1"), byCodeforces);
        assertEquals(List.of("beta1", "alpha0"), byLeetcode);
    }

    @Test
    @DisplayName("A member with no rating on the chosen platform sorts last, not first")
    void unratedOnTheChosenPlatformSortsLast() {
        persist("rated", ClubRole.STUDENT, 1500, 1600);
        persist("unrated", ClubRole.STUDENT, 1900, null);

        List<LeaderboardResponseDto> entries = board(LeaderboardPlatform.LEETCODE, LeaderboardFilter.ALL);

        assertEquals(1600, entries.get(0).rating());
        assertEquals(null, entries.get(1).rating(),
                "NULLS LAST has to survive the CASE that picks the column");
    }

    @Test
    @DisplayName("The club role reaches the response, so the frontend can badge it")
    void clubRoleIsCarriedThrough() {
        persist("core", ClubRole.CORE, 1800, null);

        assertEquals("CORE", board(LeaderboardPlatform.CODEFORCES, LeaderboardFilter.ALL)
                .get(0).clubRole());
    }

    private List<LeaderboardResponseDto> board(LeaderboardPlatform platform, LeaderboardFilter filter) {
        return leaderboardService.getLeaderboard(0, 20, platform, filter).content();
    }

    private int persisted = 0;

    private void persist(String prefix, ClubRole clubRole, Integer cfRating, Integer lcRating) {
        int n = persisted++;
        User user = new User(prefix + n, prefix + n + "@example.com", "hashed", Role.ROLE_USER);
        user.setCodeforcesHandle(prefix + n);
        user.setRating(cfRating);
        user.setLeetcodeHandle(lcRating != null ? prefix + n + "_lc" : null);
        user.setLeetcodeRating(lcRating);
        user.setClubRole(clubRole);
        userRepository.save(user);
        userRepository.flush();
    }
}
