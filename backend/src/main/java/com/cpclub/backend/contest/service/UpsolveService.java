package com.cpclub.backend.contest.service;

import com.cpclub.backend.codeforces.repository.CfSolveRepository;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.contest.dto.MyUpsolvesDto;
import com.cpclub.backend.contest.dto.UpsolveMatrixDto;
import com.cpclub.backend.contest.dto.UpsolveStatus;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestProblem;
import com.cpclub.backend.contest.entity.ClubContestProblemResult;
import com.cpclub.backend.contest.entity.ClubContestResult;
import com.cpclub.backend.contest.repository.ClubContestProblemRepository;
import com.cpclub.backend.contest.repository.ClubContestProblemResultRepository;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The upsolve tracker (P1).
 *
 * <p>What counts is fixed by D12: an accepted submission <em>inside the club contest
 * on Codeforces</em>, made after the contest ended. It is matched on the club
 * contest's own Codeforces id, so solving the same problem in its original contest,
 * or on the problemset, does not count -- a mashup problem cannot be mapped back to
 * its original reliably, and "upsolve from the contest page" is a one-sentence rule.</p>
 *
 * <p>Status follows the 6-hourly submission sync, so it can trail Codeforces by up to
 * six hours; {@link MyUpsolvesDto#getCfSyncedAt()} lets the page say so.</p>
 */
@Service
@RequiredArgsConstructor
public class UpsolveService {

    private final ClubContestRepository contestRepository;
    private final ClubContestProblemRepository problemRepository;
    private final ClubContestResultRepository resultRepository;
    private final ClubContestProblemResultRepository problemResultRepository;
    private final CfSolveRepository cfSolveRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    /** Every participant against every problem, plus a club-wide upsolve rate per problem. */
    @Transactional(readOnly = true)
    public UpsolveMatrixDto getContestUpsolve(Long clubContestId) {
        ClubContest contest = contestRepository.findById(clubContestId)
                .orElseThrow(() -> new ResourceNotFoundException("Club contest not found with id: " + clubContestId));
        List<ClubContestProblem> problems = problemRepository.findByClubContestIdOrderByProblemIndexAsc(clubContestId);
        List<ClubContestResult> participants = resultRepository.findRanklist(clubContestId);
        LocalDateTime end = ContestViews.endsAt(contest);

        Set<String> solvedInContest = solvedInContest(List.of(clubContestId));
        Set<String> upsolved = upsolved(contest, participants.stream().map(r -> r.getUser().getId()).toList());

        List<UpsolveMatrixDto.Row> rows = participants.stream()
                .map(r -> UpsolveMatrixDto.Row.builder()
                        .member(ContestViews.member(r.getUser()))
                        .cells(problems.stream()
                                .map(p -> status(r.getUser().getId(), p, solvedInContest, upsolved))
                                .toList())
                        .build())
                .toList();

        List<UpsolveMatrixDto.Problem> problemDtos = new ArrayList<>();
        for (int i = 0; i < problems.size(); i++) {
            int col = i;
            long open = rows.stream().filter(r -> r.getCells().get(col) != UpsolveStatus.SOLVED_IN_CONTEST).count();
            long done = rows.stream().filter(r -> r.getCells().get(col) == UpsolveStatus.UPSOLVED).count();
            ClubContestProblem p = problems.get(i);
            problemDtos.add(UpsolveMatrixDto.Problem.builder()
                    .index(p.getProblemIndex())
                    .name(p.getName())
                    .rating(p.getRating())
                    .upsolveRate(open == 0 ? null : (double) done / open)
                    .build());
        }

        return UpsolveMatrixDto.builder()
                .clubContestId(contest.getId())
                .contestName(contest.getName())
                .endsAt(end)
                .problems(problemDtos)
                .rows(rows)
                .build();
    }

    /** The caller's unsolved problems across every ended club contest they entered. */
    @Transactional(readOnly = true)
    public MyUpsolvesDto getMyUpsolves(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        // Only finished contests: there is nothing to upsolve in one still running.
        List<ClubContest> entered = resultRepository.findByUserId(userId).stream()
                .map(ClubContestResult::getClubContest)
                .filter(c -> !now.isBefore(ContestViews.endsAt(c)))
                .sorted(Comparator.comparing(ContestViews::endsAt).reversed())
                .toList();

        List<MyUpsolvesDto.Contest> contests = new ArrayList<>();
        if (!entered.isEmpty()) {
            List<Long> ids = entered.stream().map(ClubContest::getId).toList();
            Set<String> solvedInContest = solvedInContest(ids);
            Map<Long, List<ClubContestProblem>> problems = problemRepository
                    .findByClubContestIdInOrderByProblemIndexAsc(ids).stream()
                    .collect(Collectors.groupingBy(p -> p.getClubContest().getId()));

            for (ClubContest contest : entered) {
                Set<String> upsolved = upsolved(contest, List.of(userId));
                List<MyUpsolvesDto.PendingProblem> pending = problems.getOrDefault(contest.getId(), List.of()).stream()
                        .filter(p -> status(userId, p, solvedInContest, upsolved) == UpsolveStatus.UNSOLVED)
                        .map(p -> MyUpsolvesDto.PendingProblem.builder()
                                .index(p.getProblemIndex())
                                .name(p.getName())
                                .rating(p.getRating())
                                .url(ContestViews.problemUrl(contest.getCfContestId(), p.getProblemIndex()))
                                .build())
                        .toList();
                if (!pending.isEmpty()) {
                    contests.add(MyUpsolvesDto.Contest.builder()
                            .clubContestId(contest.getId())
                            .name(contest.getName())
                            .endsAt(ContestViews.endsAt(contest))
                            .pending(pending)
                            .build());
                }
            }
        }

        return MyUpsolvesDto.builder()
                .cfSyncedAt(user.getCfSyncedAt())
                .contests(contests)
                .build();
    }

    private static UpsolveStatus status(Long userId, ClubContestProblem p, Set<String> solvedInContest, Set<String> upsolved) {
        if (solvedInContest.contains(userId + ":" + p.getId())) {
            return UpsolveStatus.SOLVED_IN_CONTEST;
        }
        if (upsolved.contains(userId + ":" + p.getProblemIndex())) {
            return UpsolveStatus.UPSOLVED;
        }
        return UpsolveStatus.UNSOLVED;
    }

    /** "userId:problemId" for each problem solved during the contest. */
    private Set<String> solvedInContest(Collection<Long> contestIds) {
        Set<String> keys = new HashSet<>();
        for (ClubContestProblemResult pr : problemResultRepository.findByContestIds(contestIds)) {
            if (pr.isSolvedInContest()) {
                keys.add(pr.getResult().getUser().getId() + ":" + pr.getProblem().getId());
            }
        }
        return keys;
    }

    /** "userId:problemIndex" for each first accepted solve inside the contest after it ended (D12). */
    private Set<String> upsolved(ClubContest contest, Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        Set<String> keys = new HashSet<>();
        for (Object[] row : cfSolveRepository.findSolvesInContestSince(
                contest.getCfContestId(), ContestViews.endsAt(contest), userIds)) {
            keys.add(row[0] + ":" + row[1]);
        }
        return keys;
    }
}
