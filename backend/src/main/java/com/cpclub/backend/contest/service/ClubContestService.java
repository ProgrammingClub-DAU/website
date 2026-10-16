package com.cpclub.backend.contest.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.contest.client.CfStandings;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient.CodeforcesApiException;
import com.cpclub.backend.contest.dto.ClubContestCreateRequest;
import com.cpclub.backend.contest.dto.ClubContestDetailDto;
import com.cpclub.backend.contest.dto.ClubContestSummaryDto;
import com.cpclub.backend.contest.dto.ContestProblemDto;
import com.cpclub.backend.contest.dto.RanklistRowDto;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestKind;
import com.cpclub.backend.contest.entity.ClubContestProblem;
import com.cpclub.backend.contest.entity.ClubContestProblemResult;
import com.cpclub.backend.contest.entity.ClubContestResult;
import com.cpclub.backend.contest.entity.ClubSeason;
import com.cpclub.backend.contest.repository.ClubContestProblemRepository;
import com.cpclub.backend.contest.repository.ClubContestProblemResultRepository;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.contest.repository.ClubSeasonRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Adding, listing and removing club contests, and reading one contest's ranklist.
 * Standings are fetched and stored by {@link ClubContestSyncService}.
 */
@Service
@RequiredArgsConstructor
public class ClubContestService {

    private final ClubContestRepository contestRepository;
    private final ClubContestProblemRepository problemRepository;
    private final ClubContestResultRepository resultRepository;
    private final ClubContestProblemResultRepository problemResultRepository;
    private final ClubSeasonRepository seasonRepository;
    private final UserRepository userRepository;
    private final CodeforcesAuthorizedClient codeforces;
    private final TransactionTemplate transaction;

    /**
     * Adds a Codeforces contest as a club contest.
     *
     * <p>The standings header is fetched once to validate the contest and to read its
     * name, start, duration and problem list -- so a typo'd id fails here, with
     * Codeforces' own explanation, rather than at the first sync. That fetch runs
     * before the write transaction opens, so no pooled connection waits on the network.</p>
     */
    public ClubContestSummaryDto create(ClubContestCreateRequest request, Long adminId) {
        int cfId = request.getCfContestId();
        ClubContestKind kind = request.getKind();
        boolean gymId = CodeforcesAuthorizedClient.isGym(cfId);

        // Codeforces numbers gym contests from 100000, and every mashup and group
        // contest is a gym. A mismatch is a wrong id or a wrong kind -- either way the
        // admin should hear about it before anything is fetched.
        if (kind == ClubContestKind.OFFICIAL && gymId) {
            throw new BadRequestException("Official Codeforces rounds have ids below "
                    + CodeforcesAuthorizedClient.GYM_ID_FLOOR + "; this looks like a gym contest");
        }
        if (kind != ClubContestKind.OFFICIAL && !gymId) {
            throw new BadRequestException("Gym, mashup and group contests have ids of "
                    + CodeforcesAuthorizedClient.GYM_ID_FLOOR + " or more; this looks like an official round");
        }
        if (kind != ClubContestKind.OFFICIAL && !codeforces.isConfigured()) {
            throw new BadRequestException("Codeforces API key not configured");
        }
        if (contestRepository.existsByCfContestId(cfId)) {
            throw new ConflictException("Codeforces contest " + cfId + " is already a club contest");
        }

        ClubSeason season = request.getSeasonId() == null ? null
                : seasonRepository.findById(request.getSeasonId())
                        .orElseThrow(() -> new ResourceNotFoundException("Season not found with id: " + request.getSeasonId()));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + adminId));

        CfStandings header;
        try {
            header = codeforces.contestHeader(cfId);
        } catch (CodeforcesApiException e) {
            throw new BadRequestException("Codeforces: " + e.getMessage());
        }
        CfStandings.Contest cf = header.getContest();
        if (cf == null || cf.getStartTimeSeconds() == null || cf.getDurationSeconds() == null) {
            throw new BadRequestException("Codeforces has no start time for contest " + cfId + " yet");
        }

        return transaction.execute(status -> store(cfId, kind, season, admin, header));
    }

    private ClubContestSummaryDto store(int cfId, ClubContestKind kind, ClubSeason season, User admin, CfStandings header) {
        CfStandings.Contest cf = header.getContest();
        ClubContest contest = contestRepository.save(ClubContest.builder()
                .cfContestId(cfId)
                .season(season)
                .kind(kind)
                .name(cf.getName())
                // Epoch seconds from the API, stored as UTC (D31).
                .startsAt(LocalDateTime.ofEpochSecond(cf.getStartTimeSeconds(), 0, ZoneOffset.UTC))
                .durationSeconds(Math.toIntExact(cf.getDurationSeconds()))
                .createdBy(admin)
                .build());

        List<ClubContestProblem> problems = header.getProblems().stream()
                .map(p -> ClubContestProblem.builder()
                        .clubContest(contest)
                        .problemIndex(p.getIndex())
                        .name(p.getName())
                        .rating(p.getRating())
                        .build())
                .toList();
        problemRepository.saveAll(problems);

        return ContestViews.summary(contest, 0);
    }

    @Transactional
    public void delete(Long id) {
        ClubContest contest = find(id);
        // Explicit, because the H2 test schema has no ON DELETE CASCADE.
        problemResultRepository.deleteByContestId(id);
        resultRepository.deleteByContestId(id);
        problemRepository.deleteAll(problemRepository.findByClubContestIdOrderByProblemIndexAsc(id));
        contestRepository.delete(contest);
    }

    @Transactional(readOnly = true)
    public List<ClubContestSummaryDto> list() {
        Map<Long, Long> participants = new HashMap<>();
        for (Object[] row : resultRepository.countPerContest()) {
            participants.put((Long) row[0], (Long) row[1]);
        }
        return contestRepository.findAllWithSeasonNewestFirst().stream()
                .map(c -> ContestViews.summary(c, participants.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    /** The club-only ranklist: club members, ranked among themselves. */
    @Transactional(readOnly = true)
    public ClubContestDetailDto getRanklist(Long id) {
        ClubContest contest = find(id);
        List<ClubContestProblem> problems = problemRepository.findByClubContestIdOrderByProblemIndexAsc(id);
        List<ClubContestResult> results = resultRepository.findRanklist(id);

        Map<Long, Map<Long, ClubContestProblemResult>> outcomes = problemResultRepository
                .findByContestIds(List.of(id)).stream()
                .collect(Collectors.groupingBy(pr -> pr.getResult().getId(),
                        Collectors.toMap(pr -> pr.getProblem().getId(), pr -> pr)));

        List<RanklistRowDto> rows = results.stream().map(r -> {
            Map<Long, ClubContestProblemResult> mine = outcomes.getOrDefault(r.getId(), Map.of());
            List<RanklistRowDto.Cell> cells = problems.stream().map(p -> {
                ClubContestProblemResult o = mine.get(p.getId());
                return RanklistRowDto.Cell.builder()
                        .problemIndex(p.getProblemIndex())
                        .solved(o != null && o.isSolvedInContest())
                        .attempted(o != null)
                        .build();
            }).toList();
            return RanklistRowDto.builder()
                    .rank(r.getContestRank())
                    .member(ContestViews.member(r.getUser()))
                    .cfPoints(r.getCfPoints())
                    .penalty(r.getPenalty())
                    .solvedCount(r.getSolvedCount())
                    .cells(cells)
                    .build();
        }).toList();

        return ClubContestDetailDto.builder()
                .contest(ContestViews.summary(contest, results.size()))
                .problems(problems.stream().map(p -> ContestProblemDto.builder()
                        .id(p.getId())
                        .index(p.getProblemIndex())
                        .name(p.getName())
                        .rating(p.getRating())
                        .build()).toList())
                .rows(rows)
                .build();
    }

    ClubContest find(Long id) {
        return contestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club contest not found with id: " + id));
    }
}
