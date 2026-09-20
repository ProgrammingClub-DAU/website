package com.cpclub.backend.compete.service;

import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfSubmission;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.compete.dto.MatchCreationDto;
import com.cpclub.backend.compete.dto.MatchResponseDto;
import com.cpclub.backend.compete.dto.SolveReportDto;
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

    /** Returns the effective match duration in minutes, honoring timeoutMinutes when set. */
    private int effectiveLimit(Match match) {
        if (match.getTimeoutMinutes() != null && match.getTimeoutMinutes() > 0) {
            return match.getTimeoutMinutes();
        }
        return match.getDurationMinutes();
    }

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
                List<CfSubmission> submissions = codeforcesApiClient.userStatus(handle, 1, 10000);
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

        // Use frontend-provided solvedKeys instead of calling Codeforces API from the server
        Set<String> solvedKeys = dto.getSolvedKeys() != null ? new HashSet<>(dto.getSolvedKeys()) : new HashSet<>();

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

        if (nowUtc.isAfter(match.getStartTime().plusMinutes(effectiveLimit(match)))) return;

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


    /**
     * Accepts a solve report from the frontend.
     * The browser polls Codeforces directly and reports detected solves here.
     * This eliminates backend Codeforces API calls during live gameplay.
     */
    @Transactional
    public MatchResponseDto reportSolve(String matchId, SolveReportDto dto) {
        // Input validation — prevent null keys corrupting solve log
        if (dto.getContestId() == null || dto.getIndex() == null || dto.getHandle() == null
                || dto.getHandle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields: handle, contestId, index");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        if (match.getSolveLogs() == null) match.setSolveLogs(new ArrayList<>());

        // Validate match is still active (honor timeoutMinutes if set)
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (nowUtc.isAfter(match.getStartTime().plusMinutes(effectiveLimit(match)))) {
            return mapToDto(match); // Match ended, ignore solve
        }

        // Validate the handle belongs to a team in this match
        Team team = match.getTeams().stream()
            .filter(t -> t.getMembers().stream().anyMatch(m -> m.getHandle().equalsIgnoreCase(dto.getHandle())))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Handle not in this match"));

        // Validate the problem is on the active board
        String solveKey = dto.getContestId() + "-" + dto.getIndex();
        Problem matched = match.getProblems().stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive())
                && p.getId().getContestId().equals(dto.getContestId())
                && p.getId().getIndex().equals(dto.getIndex()))
            .findFirst()
            .orElse(null);

        if (matched == null) return mapToDto(match); // Problem not on board or already inactive

        // Validate submission timestamp is after match start
        if (dto.getCreationTimeSeconds() != null) {
            LocalDateTime subTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(dto.getCreationTimeSeconds()), ZoneOffset.UTC);
            if (subTime.isBefore(match.getStartTime())) return mapToDto(match);
        }

        // Check for duplicate solve (idempotent)
        boolean alreadySolved = match.getSolveLogs().stream()
            .anyMatch(l -> l.getContestId().equals(dto.getContestId()) && l.getIndex().equals(dto.getIndex()));
        if (alreadySolved) return mapToDto(match);

        try {
            SolveLog sl = new SolveLog();
            sl.setHandle(dto.getHandle());
            sl.setTeam(team.getColor());
            sl.setTimestamp(dto.getCreationTimeSeconds() != null
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(dto.getCreationTimeSeconds()), ZoneOffset.UTC)
                : nowUtc);
            sl.setContestId(dto.getContestId());
            sl.setIndex(dto.getIndex());
            sl.setMatch(match);
            sl.setProblem(matched);
            match.getSolveLogs().add(sl);

            // Handle Replace mode
            if (match.getMode() == MatchMode.replace) {
                matched.setActive(false);

                int increment = match.getReplaceIncrement() != null ? match.getReplaceIncrement() : 100;
                int newTarget = Math.min(3500, (matched.getRating() != null ? matched.getRating() : 0) + increment);

                // Use frontend-provided solvedKeys for replacement filtering
                Set<String> solvedKeys = dto.getSolvedKeys() != null ? new HashSet<>(dto.getSolvedKeys()) : new HashSet<>();
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
                } else {
                    log.warn("No replacement found for position {}, reactivating original", matched.getPosition());
                    matched.setActive(true);
                    match.getSolveLogs().remove(sl);
                }
            }

            // SERVER-SIDE WINNER DETECTION — check after every solve
            // This prevents any client from cheating by calling PATCH /duration
            String winnerTeam = detectWinner(match);
            if (winnerTeam != null) {
                log.info("Match {} won by team {} — locking match", matchId, winnerTeam);
                match.setDurationMinutes(1); // Set duration so the match appears ended to all clients
            }

        } catch (DataIntegrityViolationException e) {
            log.debug("Duplicate solve detected for {}/{} — skipping", dto.getContestId(), dto.getIndex());
        }

        matchRepository.save(match);
        return mapToDto(match);
    }

    /**
     * Detects a winner by checking rows, columns, and both diagonals.
     * Returns the winning team color, or null if no winner yet.
     */
    private String detectWinner(Match match) {
        int size = match.getGridSize() != null ? match.getGridSize() : 5;

        // Build position -> team map from active solve logs
        Map<Integer, String> positionToTeam = new HashMap<>();
        List<Problem> activeProblems = match.getProblems().stream()
            .filter(p -> Boolean.TRUE.equals(p.getActive()) || p.getPosition() != null)
            .collect(Collectors.toList());

        for (SolveLog sl : match.getSolveLogs()) {
            // Find problem by contestId+index to get its position
            activeProblems.stream()
                .filter(p -> p.getId().getContestId().equals(sl.getContestId())
                          && p.getId().getIndex().equals(sl.getIndex()))
                .findFirst()
                .ifPresent(p -> positionToTeam.put(p.getPosition(), sl.getTeam()));
        }
        // Also check inactive (replaced) problems that were solved
        match.getProblems().stream()
            .filter(p -> !Boolean.TRUE.equals(p.getActive()))
            .forEach(p -> match.getSolveLogs().stream()
                .filter(sl -> sl.getContestId().equals(p.getId().getContestId())
                           && sl.getIndex().equals(p.getId().getIndex()))
                .findFirst()
                .ifPresent(sl -> positionToTeam.putIfAbsent(p.getPosition(), sl.getTeam())));

        // Check rows
        for (int row = 0; row < size; row++) {
            String rowTeam = positionToTeam.get(row * size);
            if (rowTeam == null) continue;
            boolean wins = true;
            for (int col = 1; col < size; col++) {
                if (!rowTeam.equals(positionToTeam.get(row * size + col))) { wins = false; break; }
            }
            if (wins) return rowTeam;
        }

        // Check columns
        for (int col = 0; col < size; col++) {
            String colTeam = positionToTeam.get(col);
            if (colTeam == null) continue;
            boolean wins = true;
            for (int row = 1; row < size; row++) {
                if (!colTeam.equals(positionToTeam.get(row * size + col))) { wins = false; break; }
            }
            if (wins) return colTeam;
        }

        // Check main diagonal (top-left to bottom-right)
        String diagTeam = positionToTeam.get(0);
        if (diagTeam != null) {
            boolean wins = true;
            for (int i = 1; i < size; i++) {
                if (!diagTeam.equals(positionToTeam.get(i * size + i))) { wins = false; break; }
            }
            if (wins) return diagTeam;
        }

        // Check anti-diagonal (top-right to bottom-left)
        String antiTeam = positionToTeam.get(size - 1);
        if (antiTeam != null) {
            boolean wins = true;
            for (int i = 1; i < size; i++) {
                if (!antiTeam.equals(positionToTeam.get(i * size + (size - 1 - i)))) { wins = false; break; }
            }
            if (wins) return antiTeam;
        }

        return null;
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
                    .contestId(l.getContestId())
                    .index(l.getIndex())
                    .name(l.getProblem() != null ? l.getProblem().getName() : null)
                    .position(l.getProblem() != null ? l.getProblem().getPosition() : null)
                    .build())
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
