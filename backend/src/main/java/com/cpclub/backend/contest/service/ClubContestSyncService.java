package com.cpclub.backend.contest.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.contest.client.CfStandings;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestProblem;
import com.cpclub.backend.contest.entity.ClubContestProblemResult;
import com.cpclub.backend.contest.entity.ClubContestProblemResultId;
import com.cpclub.backend.contest.entity.ClubContestResult;
import com.cpclub.backend.contest.repository.ClubContestProblemRepository;
import com.cpclub.backend.contest.repository.ClubContestProblemResultRepository;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fetches a club contest's standings and stores the club members' results.
 *
 * <p>The run is a replace, not a merge: every result for the contest is deleted and
 * re-inserted inside one transaction. A resync is therefore idempotent, a rejudge
 * that changes an outcome is picked up, and a reader never sees a half-written
 * ranklist.</p>
 *
 * <p>The Codeforces fetch happens <em>before</em> that transaction opens, not inside
 * it. An official round's standings take several seconds to stream, and holding a
 * pooled database connection idle for the length of a download is how a small pool
 * runs dry.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClubContestSyncService {

    /**
     * Who counts. A member who entered the contest for real, rated or not; practice,
     * virtual and manager rows are not a contest result.
     */
    static final Set<String> COUNTED_PARTICIPANT_TYPES = Set.of("CONTESTANT", "OUT_OF_COMPETITION");

    /** Higher points first, then lower penalty. */
    static final Comparator<CfStandings.Row> BETTER_FIRST = Comparator
            .comparingDouble(ClubContestSyncService::points).reversed()
            .thenComparingInt(r -> r.getPenalty() == null ? 0 : r.getPenalty());

    private final ClubContestRepository contestRepository;
    private final ClubContestProblemRepository problemRepository;
    private final ClubContestResultRepository resultRepository;
    private final ClubContestProblemResultRepository problemResultRepository;
    private final UserRepository userRepository;
    private final CodeforcesAuthorizedClient codeforces;
    private final Clock clock;
    private final TransactionTemplate transaction;

    /** Returns the number of club members with a result after the sync. */
    public int sync(Long clubContestId) {
        ClubContest contest = contestRepository.findById(clubContestId)
                .orElseThrow(() -> new ResourceNotFoundException("Club contest not found with id: " + clubContestId));

        Map<String, User> byHandle = new HashMap<>();
        for (User u : userRepository.findByCodeforcesHandleIsNotNull()) {
            if (!u.getCodeforcesHandle().isBlank()) {
                byHandle.putIfAbsent(u.getCodeforcesHandle().trim().toLowerCase(Locale.ROOT), u);
            }
        }

        // Network first, outside any transaction.
        List<Placed> placed = List.of();
        List<CfStandings.Problem> cfProblems = List.of();
        if (!byHandle.isEmpty()) {
            CfStandings standings = codeforces.contestStandings(contest.getCfContestId(),
                    byHandle.values().stream().map(User::getCodeforcesHandle).toList());
            cfProblems = standings.getProblems();
            placed = rankAmongClub(bestRowPerMember(standings.getRows(), byHandle));
        }

        List<Placed> toStore = placed;
        List<CfStandings.Problem> problemOrder = cfProblems;
        transaction.executeWithoutResult(status -> replace(clubContestId, toStore, problemOrder));
        log.info("Club contest {} (CF {}) synced: {} club member(s)", clubContestId, contest.getCfContestId(), placed.size());
        return placed.size();
    }

    private void replace(Long clubContestId, List<Placed> placed, List<CfStandings.Problem> cfProblems) {
        ClubContest contest = contestRepository.findById(clubContestId)
                .orElseThrow(() -> new ResourceNotFoundException("Club contest not found with id: " + clubContestId));

        // Per-problem rows first: the H2 test schema has no ON DELETE CASCADE.
        problemResultRepository.deleteByContestId(clubContestId);
        resultRepository.deleteByContestId(clubContestId);

        Map<String, ClubContestProblem> problemsByIndex = problemRepository
                .findByClubContestIdOrderByProblemIndexAsc(clubContestId).stream()
                .collect(Collectors.toMap(ClubContestProblem::getProblemIndex, Function.identity()));

        for (Placed p : placed) {
            CfStandings.Row row = p.row();
            ClubContestResult result = resultRepository.save(ClubContestResult.builder()
                    .clubContest(contest)
                    // The members were read before the transaction opened; take a
                    // reference inside it rather than attach a detached entity.
                    .user(userRepository.getReferenceById(p.user().getId()))
                    .contestRank(p.rank())
                    .cfPoints(BigDecimal.valueOf(points(row)).setScale(2, RoundingMode.HALF_UP))
                    .penalty(row.getPenalty() == null ? 0 : row.getPenalty())
                    .solvedCount((int) row.getProblemResults().stream().filter(CfStandings.ProblemResult::isSolved).count())
                    .build());

            // problemResults line up with the standings' problem list, entry for entry.
            List<ClubContestProblemResult> outcomes = new ArrayList<>();
            for (int i = 0; i < row.getProblemResults().size() && i < cfProblems.size(); i++) {
                CfStandings.ProblemResult pr = row.getProblemResults().get(i);
                ClubContestProblem problem = problemsByIndex.get(cfProblems.get(i).getIndex());
                if (problem == null || !pr.isAttempted()) {
                    continue;
                }
                outcomes.add(new ClubContestProblemResult(
                        new ClubContestProblemResultId(result.getId(), problem.getId()),
                        result, problem, pr.isSolved()));
            }
            problemResultRepository.saveAll(outcomes);
        }

        contest.setLastSyncedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        contestRepository.save(contest);
    }

    /**
     * One row per club member. A member in a team row is credited with the team's
     * result; if somehow two counted rows match one member, the better one stands.
     */
    static Map<User, CfStandings.Row> bestRowPerMember(List<CfStandings.Row> rows, Map<String, User> byHandle) {
        Map<User, CfStandings.Row> best = new LinkedHashMap<>();
        for (CfStandings.Row row : rows) {
            if (row.getParty() == null || !COUNTED_PARTICIPANT_TYPES.contains(row.getParty().getParticipantType())) {
                continue;
            }
            for (CfStandings.Member m : row.getParty().getMembers()) {
                if (m.getHandle() == null) {
                    continue;
                }
                User user = byHandle.get(m.getHandle().trim().toLowerCase(Locale.ROOT));
                if (user != null) {
                    best.merge(user, row, (a, b) -> BETTER_FIRST.compare(a, b) <= 0 ? a : b);
                }
            }
        }
        return best;
    }

    /**
     * Competition ranking among club members only: equal points and penalty share a
     * rank, and the next distinct result skips past them (1, 1, 3).
     */
    static List<Placed> rankAmongClub(Map<User, CfStandings.Row> rows) {
        List<Map.Entry<User, CfStandings.Row>> sorted = new ArrayList<>(rows.entrySet());
        sorted.sort(Map.Entry.<User, CfStandings.Row>comparingByValue(BETTER_FIRST)
                .thenComparing(e -> e.getKey().getName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        List<Placed> placed = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            CfStandings.Row row = sorted.get(i).getValue();
            int rank = (i > 0 && BETTER_FIRST.compare(sorted.get(i - 1).getValue(), row) == 0)
                    ? placed.get(i - 1).rank()
                    : i + 1;
            placed.add(new Placed(sorted.get(i).getKey(), row, rank));
        }
        return placed;
    }

    private static double points(CfStandings.Row row) {
        return row.getPoints() == null ? 0.0 : row.getPoints();
    }

    record Placed(User user, CfStandings.Row row, int rank) {
    }

    /**
     * When a contest is due a sync, given the 15-minute schedule:
     * <ul>
     *   <li>every run while it is live, and for 30 minutes after it ends;</li>
     *   <li>once at end + 2 hours, when final standings have settled;</li>
     *   <li>then once a day for three days, to catch rejudges.</li>
     * </ul>
     * Expressed as checkpoints rather than clock windows, so a missed run -- the
     * service was restarting at end + 2 hours, say -- is caught up on the next tick
     * instead of skipped.
     */
    static boolean needsSync(LocalDateTime startsAt, int durationSeconds, LocalDateTime lastSyncedAt, LocalDateTime now) {
        if (now.isBefore(startsAt)) {
            return false;
        }
        LocalDateTime end = startsAt.plusSeconds(durationSeconds);
        if (!now.isAfter(end.plusMinutes(30))) {
            return true;
        }
        for (LocalDateTime checkpoint : List.of(end.plusHours(2), end.plusDays(1), end.plusDays(2), end.plusDays(3))) {
            if (!now.isBefore(checkpoint) && (lastSyncedAt == null || lastSyncedAt.isBefore(checkpoint))) {
                return true;
            }
        }
        return false;
    }
}
