package com.cpclub.backend.compete.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.compete.dto.MatchCreationDto;
import com.cpclub.backend.compete.dto.MatchResponseDto;
import com.cpclub.backend.compete.entity.*;
import com.cpclub.backend.compete.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompeteMatchService {

    private static final int POLL_COOLDOWN_SECONDS = 20;

    private final MatchRepository matchRepository;
    private final CfProblemRepository cfProblemRepository;
    private final CodeforcesApiClient codeforcesApiClient;

    private String generateMatchId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private Set<String> fetchSolvedProblemKeys(List<String> handles) {
        Set<String> solvedSet = new HashSet<>();
        for (String handle : handles) {
            try {
                List<CfSubmission> submissions = codeforcesApiClient.userStatus(handle, 1, 2000);
                for (CfSubmission sub : submissions) {
                    if ("OK".equals(sub.getVerdict()) && sub.getProblem() != null && sub.getProblem().getContestId() != null)
                        solvedSet.add(sub.getProblem().getContestId() + "-" + sub.getProblem().getIndex());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch submissions for handle {} during problem generation", handle, e);
            }
        }
        return solvedSet;
    }

    @Transactional
    public MatchResponseDto createMatch(MatchCreationDto dto) {
        // Bug #2 fix: validate gridSize server-side (reference: createMatch.ts line 45)
        if (!List.of(3, 4, 5, 6).contains(dto.getGridSize()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gridSize must be 3, 4, 5, or 6");

        String matchId = generateMatchId();

        // Bug #1 fix: convert Instant (UTC from frontend toISOString()) -> LocalDateTime UTC
        LocalDateTime startTime = LocalDateTime.ofInstant(dto.getStartTime(), ZoneOffset.UTC);

        Match match = new Match();
        match.setId(matchId);
        match.setMode(dto.getMode());
        match.setStartTime(startTime);
        match.setDurationMinutes(dto.getDurationMinutes());
        match.setMinRating(dto.getMinRating());
        match.setMaxRating(dto.getMaxRating());
        match.setGridSize(dto.getGridSize());
        match.setReplaceIncrement(dto.getMode() == MatchMode.replace ? dto.getReplaceIncrement() : null);
        match.setTimeoutMinutes(dto.getTimeoutMinutes());
        match.setShowRatings(dto.getShowRatings() != null ? dto.getShowRatings() : Boolean.TRUE);
        match.setLastPolledAt(LocalDateTime.now(ZoneOffset.UTC));
        match.setSolveLogs(new ArrayList<>()); // Bug #5 fix: init list to avoid NPE on first poll

        match = matchRepository.save(match);

        List<Team> teams = new ArrayList<>();
        List<String> allHandles = new ArrayList<>();
        for (MatchCreationDto.TeamDto tDto : dto.getTeams()) {
            Team t = new Team();
            t.setName(tDto.getName());
            t.setColor(tDto.getColor());
            t.setMatch(match);
            List<Member> members = new ArrayList<>();
            for (String h : tDto.getMembers()) {
                Member m = new Member();
                m.setHandle(h);
                m.setTeam(t);
                members.add(m);
                allHandles.add(h);
            }
            t.setMembers(members);
            teams.add(t);
        }
        match.setTeams(teams);

        // Bug #3: Filter solved AND *special problems (query already excludes *special via CfProblemRepository)
        Set<String> solvedKeys = fetchSolvedProblemKeys(allHandles);

        int problemCount = dto.getGridSize() * dto.getGridSize();
        List<com.cpclub.backend.codeforces.entity.CfProblem> pool =
            cfProblemRepository.findRandomProblemsByRatingRange(dto.getMinRating(), dto.getMaxRating(), problemCount * 10);

        List<com.cpclub.backend.codeforces.entity.CfProblem> valid = pool.stream()
            .filter(p -> p.getContestId() != null && !solvedKeys.contains(p.getContestId() + "-" + p.getProblemIndex()))
            .limit(problemCount)
            .collect(Collectors.toList());

        if (valid.size() < problemCount) {
            List<com.cpclub.backend.codeforces.entity.CfProblem> fallback = pool.stream()
                .filter(p -> p.getContestId() != null).limit(problemCount).collect(Collectors.toList());
            if (fallback.size() < problemCount)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough problems found in this rating range");
            valid = fallback;
        }

        List<Problem> problems = new ArrayList<>();
        for (int i = 0; i < problemCount; i++) {
            com.cpclub.backend.codeforces.entity.CfProblem cfP = valid.get(i);
            Problem p = new Problem();
            p.setId(new ProblemId(cfP.getContestId(), cfP.getProblemIndex(), matchId));
            p.setMatch(match);
            p.setName(cfP.getName());
            p.setRating(cfP.getRating());
            p.setPosition(i);
            p.setActive(true);
            problems.add(p);
        }
        match.setProblems(problems);
        match = matchRepository.save(match);
        return mapToDto(match);
    }

    @Transactional(readOnly = true)
    public MatchResponseDto getMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        return mapToDto(match);
    }

    @Transactional
    public void pollMatch(String matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        // Bug #5 fix: null guard solveLogs BEFORE any use
        if (match.getSolveLogs() == null) match.setSolveLogs(new ArrayList<>());

        // Bug #11 fix: all time comparisons in UTC
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);

        if (nowUtc.isAfter(match.getStartTime().plusMinutes(match.getDurationMinutes()))) return;

        // Bug #9 fix: 20-second Codeforces rate-limit cooldown (ref: poll-submissions.ts)
        if (match.getLastPolledAt() != null && match.getLastPolledAt().plusSeconds(POLL_COOLDOWN_SECONDS).isAfter(nowUtc)) {
            return;
        }

        List<String> handles = match.getTeams().stream()
                .flatMap(t -> t.getMembers().stream()).map(Member::getHandle).toList();

        Map<String, Problem> activeLookup = new HashMap<>();
        for (Problem p : match.getProblems()) {
            if (Boolean.TRUE.equals(p.getActive()))
                activeLookup.put(p.getId().getContestId() + "-" + p.getId().getIndex(), p);
        }

        boolean changed = false;

        for (String handle : handles) {
            try {
                List<CfSubmission> submissions = codeforcesApiClient.userStatus(handle, 1, 20);
                for (CfSubmission sub : submissions) {
                    if (!"OK".equals(sub.getVerdict())) continue;
                    if (sub.getProblem() == null || sub.getProblem().getContestId() == null) continue;

                    // Bug #11 fix: explicit UTC conversion of Codeforces epoch timestamp
                    LocalDateTime subTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(sub.getCreationTimeSeconds()), ZoneOffset.UTC);
                    if (subTime.isBefore(match.getStartTime())) continue;

                    String key = sub.getProblem().getContestId() + "-" + sub.getProblem().getIndex();
                    Problem matched = activeLookup.get(key);
                    if (matched == null) continue;

                    Team team = match.getTeams().stream()
                        .filter(t -> t.getMembers().stream().anyMatch(m -> m.getHandle().equals(handle)))
                        .findFirst().orElse(null);
                    if (team == null) continue;

                    boolean alreadySolved = match.getSolveLogs().stream()
                        .anyMatch(l -> l.getContestId().equals(matched.getId().getContestId()) &&
                                       l.getIndex().equals(matched.getId().getIndex()));
                    if (alreadySolved) continue;

                    try {
                        SolveLog sl = new SolveLog();
                        sl.setHandle(handle);
                        sl.setTeam(team.getColor()); // Bug #8 fix: store COLOR not name
                        sl.setTimestamp(subTime);
                        sl.setContestId(matched.getId().getContestId());
                        sl.setIndex(matched.getId().getIndex());
                        sl.setMatch(match);
                        sl.setProblem(matched);
                        match.getSolveLogs().add(sl);
                        changed = true;

                        if (match.getMode() == MatchMode.replace) {
                            matched.setActive(false);
                            activeLookup.remove(key);

                            int increment = match.getReplaceIncrement() != null ? match.getReplaceIncrement() : 100;
                            int newTarget = Math.min(3500, (matched.getRating() != null ? matched.getRating() : 0) + increment);

                            Set<String> solvedKeys = fetchSolvedProblemKeys(handles);
                            List<com.cpclub.backend.codeforces.entity.CfProblem> pool =
                                cfProblemRepository.findRandomProblemsByRatingRange(newTarget, newTarget, 20);
                            Set<String> existingKeys = match.getProblems().stream()
                                .map(p -> p.getId().getContestId() + "-" + p.getId().getIndex())
                                .collect(Collectors.toSet());

                            com.cpclub.backend.codeforces.entity.CfProblem replacement = pool.stream()
                                .filter(p -> p.getContestId() != null &&
                                             !existingKeys.contains(p.getContestId() + "-" + p.getProblemIndex()) &&
                                             !solvedKeys.contains(p.getContestId() + "-" + p.getProblemIndex()))
                                .findFirst()
                                .orElseGet(() -> pool.stream()
                                    .filter(p -> p.getContestId() != null && !existingKeys.contains(p.getContestId() + "-" + p.getProblemIndex()))
                                    .findFirst().orElse(null));

                            if (replacement != null) {
                                Problem newP = new Problem();
                                newP.setId(new ProblemId(replacement.getContestId(), replacement.getProblemIndex(), matchId));
                                newP.setMatch(match);
                                newP.setName(replacement.getName());
                                newP.setRating(replacement.getRating());
                                newP.setPosition(matched.getPosition());
                                newP.setActive(true);
                                match.getProblems().add(newP);
                                activeLookup.put(replacement.getContestId() + "-" + replacement.getProblemIndex(), newP);
                            } else {
                                // Bug #12 fix: no replacement found — reactivate so grid doesn't shrink
                                log.warn("No replacement found for position {}, reactivating original", matched.getPosition());
                                matched.setActive(true);
                                activeLookup.put(key, matched);
                                match.getSolveLogs().remove(sl);
                            }
                        }
                    } catch (DataIntegrityViolationException e) {
                        // Bug #4 fix: unique DB constraint caught — another concurrent request handled this
                        log.debug("Duplicate solve detected for {}/{} — skipping", matched.getId().getContestId(), matched.getId().getIndex());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to poll handle {}", handle, e);
            }
        }

        match.setLastPolledAt(nowUtc); // always update cooldown timestamp
        matchRepository.save(match);
    }

    // Bug #10 fix: allow winning client to propagate match end to all other clients
    @Transactional
    public void setMatchDuration(String matchId, int durationMinutes) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));
        match.setDurationMinutes(durationMinutes);
        matchRepository.save(match);
    }

    private MatchResponseDto mapToDto(Match match) {
        // Bug #6/#11 fix: emit startTime as ISO Instant string so frontend new Date() works correctly
        Instant startInstant = match.getStartTime() != null ? match.getStartTime().toInstant(ZoneOffset.UTC) : null;

        MatchResponseDto.MatchResponseDtoBuilder b = MatchResponseDto.builder()
            .id(match.getId())
            .startTime(startInstant)
            .durationMinutes(match.getDurationMinutes())
            .mode(match.getMode())
            .replaceIncrement(match.getReplaceIncrement())
            .gridSize(match.getGridSize())
            .timeoutMinutes(match.getTimeoutMinutes())
            .showRatings(match.getShowRatings());

        b.teams((match.getTeams() == null) ? List.of() : match.getTeams().stream().map(t ->
            MatchResponseDto.TeamDto.builder()
                .name(t.getName()).color(t.getColor())
                .members(t.getMembers().stream().map(Member::getHandle).toList())
                .build()).toList());

        b.solveLog((match.getSolveLogs() == null) ? List.of() : match.getSolveLogs().stream().map(l ->
            MatchResponseDto.SolveEntryDto.builder()
                .handle(l.getHandle()).team(l.getTeam())
                .timestamp(l.getTimestamp() != null ? l.getTimestamp().toInstant(ZoneOffset.UTC) : null)
                .problem(MatchResponseDto.ProblemRefDto.builder()
                    .contestId(l.getContestId()).index(l.getIndex()).build())
                .build()).toList());

        b.problems((match.getProblems() == null) ? List.of() : match.getProblems().stream().map(p -> {
            int pos = p.getPosition();
            String solvedBy = null, claimedBy = null;
            if (match.getSolveLogs() != null) {
                Optional<SolveLog> sl = match.getSolveLogs().stream()
                    .filter(l -> l.getContestId().equals(p.getId().getContestId()) && l.getIndex().equals(p.getId().getIndex()))
                    .min(Comparator.comparing(SolveLog::getTimestamp));
                if (sl.isPresent()) { solvedBy = sl.get().getTeam(); claimedBy = sl.get().getHandle(); }
            }
            return MatchResponseDto.ProblemCellDto.builder()
                .row(pos / match.getGridSize()).col(pos % match.getGridSize())
                .contestId(p.getId().getContestId()).index(p.getId().getIndex())
                .name(p.getName()).rating(p.getRating())
                .link("https://codeforces.com/contest/" + p.getId().getContestId() + "/problem/" + p.getId().getIndex())
                .active(p.getActive()).position(pos)
                .solvedBy(solvedBy).claimedBy(claimedBy).build();
        }).toList());

        return b.build();
    }
}
