package com.cpclub.backend.contest.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.contest.dto.ChampionshipDto;
import com.cpclub.backend.contest.dto.SeasonDto;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestResult;
import com.cpclub.backend.contest.entity.ClubSeason;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.contest.repository.ClubSeasonRepository;
import com.cpclub.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Season standings for the Club Championship (Section 1.3), computed on read.
 *
 * <pre>
 * points(member, contest) = round(100 * (P - rank + 1) / P)
 *   P    = club members with a result in that contest
 *   rank = rank among club members only
 * season total = sum of the member's best K contest scores (K = count_best, or all)
 * ties: more contests with points, then the better best single-contest rank
 * </pre>
 *
 * <p>Computed on read rather than stored: a season has a handful of contests and a
 * few dozen entrants, and a stored table would need invalidating on every resync,
 * rejudge and deleted contest.</p>
 */
@Service
@RequiredArgsConstructor
public class ChampionshipService {

    private final ClubSeasonRepository seasonRepository;
    private final ClubContestRepository contestRepository;
    private final ClubContestResultRepository resultRepository;

    @Transactional(readOnly = true)
    public ChampionshipDto getStandings(Long seasonId) {
        ClubSeason season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResourceNotFoundException("Season not found with id: " + seasonId));
        List<ClubContest> contests = contestRepository.findBySeasonIdOrderByStartsAtAsc(seasonId);

        List<ClubContestResult> results = contests.isEmpty() ? List.of()
                : resultRepository.findByContestIds(contests.stream().map(ClubContest::getId).toList());

        List<Entry> entries = results.stream()
                .map(r -> new Entry(r.getUser(), r.getClubContest(), r.getContestRank()))
                .toList();

        return ChampionshipDto.builder()
                .season(SeasonDto.builder()
                        .id(season.getId())
                        .name(season.getName())
                        .startsOn(season.getStartsOn())
                        .endsOn(season.getEndsOn())
                        .countBest(season.getCountBest())
                        .contestCount(contests.size())
                        .build())
                .rows(standings(entries, season.getCountBest()))
                .build();
    }

    /** One member's placing in one contest. */
    record Entry(User user, ClubContest contest, int clubRank) {
    }

    static int points(int participants, int clubRank) {
        return (int) Math.round(100.0 * (participants - clubRank + 1) / participants);
    }

    static List<ChampionshipDto.Row> standings(List<Entry> entries, Integer countBest) {
        // Chronological, so each breakdown reads in season order and the stable sort
        // below resolves a tie at the best-K boundary in favour of the earlier contest.
        List<Entry> ordered = new ArrayList<>(entries);
        ordered.sort(Comparator.comparing((Entry e) -> e.contest().getStartsAt())
                .thenComparing(e -> e.contest().getId()));

        Map<Long, Integer> participants = new HashMap<>();
        for (Entry e : ordered) {
            participants.merge(e.contest().getId(), 1, Integer::sum);
        }

        Map<Long, List<ChampionshipDto.ContestScore>> byMember = new LinkedHashMap<>();
        Map<Long, User> members = new HashMap<>();
        for (Entry e : ordered) {
            members.put(e.user().getId(), e.user());
            byMember.computeIfAbsent(e.user().getId(), k -> new ArrayList<>())
                    .add(ChampionshipDto.ContestScore.builder()
                            .clubContestId(e.contest().getId())
                            .contestName(e.contest().getName())
                            .clubRank(e.clubRank())
                            .points(points(participants.get(e.contest().getId()), e.clubRank()))
                            .build());
        }

        List<ChampionshipDto.Row> rows = new ArrayList<>();
        for (Map.Entry<Long, List<ChampionshipDto.ContestScore>> m : byMember.entrySet()) {
            List<ChampionshipDto.ContestScore> scores = m.getValue();

            // Best K, ties at the boundary going to the earlier contest so the choice
            // is stable between reads.
            List<ChampionshipDto.ContestScore> ranked = new ArrayList<>(scores);
            ranked.sort(Comparator.comparingInt(ChampionshipDto.ContestScore::getPoints).reversed());
            int k = countBest == null ? ranked.size() : Math.min(countBest, ranked.size());
            long total = 0;
            for (int i = 0; i < k; i++) {
                ranked.get(i).setCounted(true);
                total += ranked.get(i).getPoints();
            }

            rows.add(ChampionshipDto.Row.builder()
                    .member(ContestViews.member(members.get(m.getKey())))
                    .total(total)
                    .contestsWithPoints((int) scores.stream().filter(s -> s.getPoints() > 0).count())
                    .bestRank(scores.stream().mapToInt(ChampionshipDto.ContestScore::getClubRank).min().orElse(0))
                    .breakdown(scores)
                    .build());
        }

        Comparator<ChampionshipDto.Row> order = Comparator
                .comparingLong(ChampionshipDto.Row::getTotal).reversed()
                .thenComparing(Comparator.comparingInt(ChampionshipDto.Row::getContestsWithPoints).reversed())
                .thenComparingInt(ChampionshipDto.Row::getBestRank);
        rows.sort(order.thenComparing(r -> r.getMember().getName(),
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        for (int i = 0; i < rows.size(); i++) {
            boolean tied = i > 0 && order.compare(rows.get(i - 1), rows.get(i)) == 0;
            rows.get(i).setRank(tied ? rows.get(i - 1).getRank() : i + 1);
        }
        return rows;
    }
}
