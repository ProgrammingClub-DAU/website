package com.cpclub.backend.contest.service;

import com.cpclub.backend.contest.dto.ChampionshipDto;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Section 1.3 Championship formula, on hand-built entries. */
class ChampionshipServiceTest {

    @Test
    @DisplayName("points = round(100 * (P - rank + 1) / P)")
    void pointsFormula() {
        assertEquals(100, ChampionshipService.points(10, 1));
        assertEquals(10, ChampionshipService.points(10, 10));
        assertEquals(67, ChampionshipService.points(3, 2), "66.67 rounds up");
        assertEquals(100, ChampionshipService.points(1, 1), "a lone entrant takes the full 100");
    }

    @Test
    @DisplayName("Five contests with count_best = 3: the best three are summed")
    void bestThreeOfFive() {
        User alice = user(1L, "Alice");
        List<ClubContest> contests = contests(5);
        List<ChampionshipService.Entry> entries = new ArrayList<>();
        // P = 4 in every contest, so rank r scores round(100 * (5 - r) / 4).
        // Alice's ranks 1, 4, 2, 3, 1 score 100, 25, 75, 50, 100.
        int[] aliceRanks = {1, 4, 2, 3, 1};
        for (int i = 0; i < 5; i++) {
            entries.add(new ChampionshipService.Entry(alice, contests.get(i), aliceRanks[i]));
            // Three filler entrants per contest at the remaining ranks.
            int filler = 0;
            for (int r = 1; r <= 4; r++) {
                if (r != aliceRanks[i]) {
                    entries.add(new ChampionshipService.Entry(user(100L + i * 10 + filler++, "F" + i + r), contests.get(i), r));
                }
            }
        }

        ChampionshipDto.Row row = ChampionshipService.standings(entries, 3).stream()
                .filter(r -> r.getMember().getId().equals(1L)).findFirst().orElseThrow();

        assertEquals(100 + 100 + 75, row.getTotal(), "best three of 100, 25, 75, 50, 100");
        assertEquals(5, row.getBreakdown().size());
        assertEquals(3, row.getBreakdown().stream().filter(ChampionshipDto.ContestScore::isCounted).count());
        assertEquals(5, row.getContestsWithPoints());
        assertEquals(1, row.getBestRank());
        // The breakdown reads in season order.
        assertEquals(List.of(1, 4, 2, 3, 1), row.getBreakdown().stream().map(ChampionshipDto.ContestScore::getClubRank).toList());
    }

    @Test
    @DisplayName("Without count_best every contest counts")
    void nullCountBestCountsEverything() {
        User alice = user(1L, "Alice");
        List<ClubContest> contests = contests(2);
        List<ChampionshipService.Entry> entries = List.of(
                new ChampionshipService.Entry(alice, contests.get(0), 1),
                new ChampionshipService.Entry(alice, contests.get(1), 1));

        assertEquals(200, ChampionshipService.standings(entries, null).get(0).getTotal());
    }

    @Test
    @DisplayName("Equal totals: more contests with points ranks higher")
    void tieBrokenByContestsWithPoints() {
        User steady = user(1L, "Steady");
        User oneShot = user(2L, "OneShot");
        List<ClubContest> contests = contests(2);
        // Contest 1: P = 2. Contest 2: P = 2.
        // steady: rank 2 + rank 2 = 50 + 50 = 100 over two contests.
        // oneShot: rank 1 in contest 1 only = 100 over one contest.
        List<ChampionshipService.Entry> entries = List.of(
                new ChampionshipService.Entry(oneShot, contests.get(0), 1),
                new ChampionshipService.Entry(steady, contests.get(0), 2),
                new ChampionshipService.Entry(user(3L, "Winner"), contests.get(1), 1),
                new ChampionshipService.Entry(steady, contests.get(1), 2));

        List<ChampionshipDto.Row> rows = ChampionshipService.standings(entries, null);
        ChampionshipDto.Row s = byName(rows, "Steady");
        ChampionshipDto.Row o = byName(rows, "OneShot");

        assertEquals(s.getTotal(), o.getTotal());
        assertTrue(s.getRank() < o.getRank(), "two contests with points beats one");
    }

    @Test
    @DisplayName("Equal totals and contests: the better best single-contest rank wins")
    void tieBrokenByBestRank() {
        User a = user(1L, "A");
        User b = user(2L, "B");
        List<ClubContest> contests = contests(2);
        // P = 4 in both. A: ranks 1 and 4 -> 100 + 25. B: ranks 2 and 3 -> 75 + 50.
        List<ChampionshipService.Entry> entries = new ArrayList<>(List.of(
                new ChampionshipService.Entry(a, contests.get(0), 1),
                new ChampionshipService.Entry(b, contests.get(0), 2),
                new ChampionshipService.Entry(a, contests.get(1), 4),
                new ChampionshipService.Entry(b, contests.get(1), 3)));
        entries.add(new ChampionshipService.Entry(user(3L, "X"), contests.get(0), 3));
        entries.add(new ChampionshipService.Entry(user(4L, "Y"), contests.get(0), 4));
        entries.add(new ChampionshipService.Entry(user(5L, "Z"), contests.get(1), 1));
        entries.add(new ChampionshipService.Entry(user(6L, "W"), contests.get(1), 2));

        List<ChampionshipDto.Row> rows = ChampionshipService.standings(entries, null);

        assertEquals(125, byName(rows, "A").getTotal());
        assertEquals(125, byName(rows, "B").getTotal());
        assertTrue(byName(rows, "A").getRank() < byName(rows, "B").getRank(), "best rank 1 beats best rank 2");
    }

    @Test
    @DisplayName("Members level on every tiebreak share a rank")
    void fullTieSharesRank() {
        List<ClubContest> contests = contests(1);
        List<ChampionshipService.Entry> entries = List.of(
                new ChampionshipService.Entry(user(1L, "A"), contests.get(0), 1),
                new ChampionshipService.Entry(user(2L, "B"), contests.get(0), 1));

        List<ChampionshipDto.Row> rows = ChampionshipService.standings(entries, null);

        assertEquals(1, rows.get(0).getRank());
        assertEquals(1, rows.get(1).getRank());
        assertFalse(rows.isEmpty());
    }

    private static ChampionshipDto.Row byName(List<ChampionshipDto.Row> rows, String name) {
        return rows.stream().filter(r -> r.getMember().getName().equals(name)).findFirst().orElseThrow();
    }

    private static List<ClubContest> contests(int n) {
        List<ClubContest> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(ClubContest.builder().id((long) i + 1).name("Contest " + (i + 1))
                    .startsAt(LocalDateTime.of(2026, 1, 1, 12, 0).plusWeeks(i)).durationSeconds(7200).build());
        }
        return list;
    }

    private static User user(Long id, String name) {
        return User.builder().id(id).name(name).email(name.toLowerCase() + "@dau.ac.in").build();
    }
}
