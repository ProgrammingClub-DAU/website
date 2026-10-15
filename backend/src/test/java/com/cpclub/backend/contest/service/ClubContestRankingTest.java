package com.cpclub.backend.contest.service;

import com.cpclub.backend.contest.client.CfStandings;
import com.cpclub.backend.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pure parts of the club contest sync: who counts, how the club is ranked, and
 * when a contest is due a sync. No database and no Codeforces.
 */
class ClubContestRankingTest {

    private final User alice = user(1L, "Alice", "alice");
    private final User bob = user(2L, "Bob", "Bob_CF");
    private final User carol = user(3L, "Carol", "carol");
    private final Map<String, User> byHandle = Map.of("alice", alice, "bob_cf", bob, "carol", carol);

    @Test
    @DisplayName("Practice, virtual and manager rows are not contest results")
    void onlyContestantAndOutOfCompetitionCount() {
        List<CfStandings.Row> rows = List.of(
                row("alice", "PRACTICE", 5, 0),
                row("Bob_CF", "VIRTUAL", 5, 0),
                row("carol", "MANAGER", 5, 0));

        assertTrue(ClubContestSyncService.bestRowPerMember(rows, byHandle).isEmpty());

        Map<User, CfStandings.Row> counted = ClubContestSyncService.bestRowPerMember(List.of(
                row("alice", "CONTESTANT", 3, 40),
                row("carol", "OUT_OF_COMPETITION", 2, 10)), byHandle);
        assertEquals(2, counted.size());
    }

    @Test
    @DisplayName("Handles are matched regardless of case")
    void handlesMatchCaseInsensitively() {
        Map<User, CfStandings.Row> counted = ClubContestSyncService.bestRowPerMember(
                List.of(row("BOB_cf", "CONTESTANT", 4, 0)), byHandle);

        assertEquals(1, counted.size());
        assertTrue(counted.containsKey(bob));
    }

    @Test
    @DisplayName("A practice row cannot displace a member's real result")
    void practiceRowDoesNotReplaceContestantRow() {
        Map<User, CfStandings.Row> counted = ClubContestSyncService.bestRowPerMember(List.of(
                row("alice", "CONTESTANT", 2, 30),
                row("alice", "PRACTICE", 7, 0)), byHandle);

        assertEquals(2.0, counted.get(alice).getPoints());
    }

    @Test
    @DisplayName("Equal points and penalty share a club rank; the next result skips past them")
    void tiesShareARank() {
        Map<User, CfStandings.Row> counted = ClubContestSyncService.bestRowPerMember(List.of(
                row("carol", "CONTESTANT", 2, 90),
                row("alice", "CONTESTANT", 3, 50),
                row("Bob_CF", "CONTESTANT", 3, 50)), byHandle);

        List<ClubContestSyncService.Placed> placed = ClubContestSyncService.rankAmongClub(counted);

        assertEquals(1, placed.get(0).rank());
        assertEquals(1, placed.get(1).rank());
        assertEquals(3, placed.get(2).rank(), "competition ranking: 1, 1, 3");
        assertEquals(carol, placed.get(2).user());
    }

    @Test
    @DisplayName("Lower penalty breaks a tie on points")
    void penaltyBreaksTies() {
        Map<User, CfStandings.Row> counted = ClubContestSyncService.bestRowPerMember(List.of(
                row("alice", "CONTESTANT", 3, 80),
                row("Bob_CF", "CONTESTANT", 3, 20)), byHandle);

        List<ClubContestSyncService.Placed> placed = ClubContestSyncService.rankAmongClub(counted);

        assertEquals(bob, placed.get(0).user());
        assertEquals(2, placed.get(1).rank());
    }

    // ------------------------------------------------------------- schedule

    private static final LocalDateTime START = LocalDateTime.of(2026, 10, 10, 12, 0);
    private static final int TWO_HOURS = 7200;
    private static final LocalDateTime END = START.plusSeconds(TWO_HOURS);

    @Test
    @DisplayName("Nothing to sync before the contest starts")
    void notBeforeStart() {
        assertFalse(due(null, START.minusMinutes(1)));
    }

    @Test
    @DisplayName("Every run while live, and for 30 minutes after it ends")
    void everyRunWhileLive() {
        assertTrue(due(START.plusMinutes(10), START.plusMinutes(15)));
        assertTrue(due(END.plusMinutes(15), END.plusMinutes(30)));
        assertFalse(due(END.plusMinutes(30), END.plusMinutes(45)), "quiet between +30m and +2h");
    }

    @Test
    @DisplayName("Once at end + 2 hours, then once a day for three days")
    void checkpointsAfterTheContest() {
        LocalDateTime lastLive = END.plusMinutes(30);
        assertTrue(due(lastLive, END.plusHours(2)));
        assertFalse(due(END.plusHours(2), END.plusHours(2).plusMinutes(15)), "only once");

        assertTrue(due(END.plusHours(2), END.plusDays(1)));
        assertTrue(due(END.plusDays(1), END.plusDays(2)));
        assertTrue(due(END.plusDays(2), END.plusDays(3)));
        assertFalse(due(END.plusDays(3), END.plusDays(4)), "finished after three days");
    }

    @Test
    @DisplayName("A missed checkpoint is caught up on the next tick, once")
    void missedCheckpointIsCaughtUp() {
        // The service was down from end + 1h until end + 2.5 days.
        LocalDateTime back = END.plusDays(2).plusHours(12);
        assertTrue(due(END.plusMinutes(30), back));
        assertFalse(due(back, back.plusMinutes(15)), "one catch-up covers every missed checkpoint");
    }

    private static boolean due(LocalDateTime lastSynced, LocalDateTime now) {
        return ClubContestSyncService.needsSync(START, TWO_HOURS, lastSynced, now);
    }

    // ------------------------------------------------------------- helpers

    private static User user(Long id, String name, String handle) {
        return User.builder().id(id).name(name).email(handle + "@dau.ac.in").codeforcesHandle(handle).build();
    }

    private static CfStandings.Row row(String handle, String type, double points, int penalty) {
        CfStandings.Member m = new CfStandings.Member();
        m.setHandle(handle);
        CfStandings.Party party = new CfStandings.Party();
        party.setMembers(List.of(m));
        party.setParticipantType(type);
        CfStandings.Row row = new CfStandings.Row();
        row.setParty(party);
        row.setPoints(points);
        row.setPenalty(penalty);
        return row;
    }
}
