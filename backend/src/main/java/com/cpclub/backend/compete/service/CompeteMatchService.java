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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompeteMatchService {

    private final MatchRepository matchRepository;
    private final CfProblemRepository cfProblemRepository;
    private final CodeforcesApiClient codeforcesApiClient;

    private String generateMatchId() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public MatchResponseDto createMatch(MatchCreationDto dto) {
        String matchId = generateMatchId();
        
        Match match = new Match();
        match.setId(matchId);
        match.setMode(dto.getMode());
        match.setStartTime(dto.getStartTime());
        match.setDurationMinutes(dto.getDurationMinutes());
        match.setMinRating(dto.getMinRating());
        match.setMaxRating(dto.getMaxRating());
        match.setGridSize(dto.getGridSize());
        match.setReplaceIncrement(dto.getReplaceIncrement());
        match.setTimeoutMinutes(dto.getTimeoutMinutes());
        match.setShowRatings(dto.getShowRatings() != null ? dto.getShowRatings() : true);
        match.setLastPolledAt(LocalDateTime.now());
        
        match = matchRepository.save(match);
        
        List<Team> teams = new ArrayList<>();
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
            }
            t.setMembers(members);
            teams.add(t);
        }
        match.setTeams(teams);
        
        int problemCount = dto.getGridSize() * dto.getGridSize();
        List<com.cpclub.backend.codeforces.entity.CfProblem> pool = 
            cfProblemRepository.findRandomProblemsByRatingRange(
                dto.getMinRating(), 
                dto.getMaxRating(), 
                problemCount * 2
            );
            
        List<com.cpclub.backend.codeforces.entity.CfProblem> valid = pool.stream()
            .filter(p -> p.getContestId() != null)
            .limit(problemCount)
            .toList();
            
        if (valid.size() < problemCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough problems found in this rating range");
        }
        
        List<Problem> problems = new ArrayList<>();
        for (int i = 0; i < problemCount; i++) {
            com.cpclub.backend.codeforces.entity.CfProblem cfP = valid.get(i);
            
            Problem p = new Problem();
            ProblemId pid = new ProblemId(cfP.getContestId(), cfP.getProblemIndex(), matchId);
            p.setId(pid);
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
        
        if (LocalDateTime.now().isAfter(match.getStartTime().plusMinutes(match.getDurationMinutes()))) {
            return;
        }

        List<String> handles = match.getTeams().stream()
                .flatMap(t -> t.getMembers().stream())
                .map(Member::getHandle)
                .toList();

        Map<String, Problem> problemLookup = new HashMap<>();
        for (Problem p : match.getProblems()) {
            problemLookup.put(p.getId().getContestId() + "-" + p.getId().getIndex(), p);
        }

        boolean newSolve = false;

        for (String handle : handles) {
            try {
                List<CfSubmission> submissions = codeforcesApiClient.userStatus(handle, 1, 20);
                
                for (CfSubmission sub : submissions) {
                    if (!"OK".equals(sub.getVerdict())) continue;
                    
                    LocalDateTime subTime = LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(sub.getCreationTimeSeconds()), ZoneId.systemDefault());
                        
                    if (subTime.isBefore(match.getStartTime())) continue;
                    
                    String key = sub.getProblem().getContestId() + "-" + sub.getProblem().getIndex();
                    Problem matchedProblem = problemLookup.get(key);
                    
                    if (matchedProblem != null && matchedProblem.getActive()) {
                        Team team = match.getTeams().stream()
                            .filter(t -> t.getMembers().stream().anyMatch(m -> m.getHandle().equals(handle)))
                            .findFirst().orElse(null);
                            
                        if (team != null) {
                            boolean alreadySolved = match.getSolveLogs().stream()
                                .anyMatch(l -> l.getContestId().equals(matchedProblem.getId().getContestId()) &&
                                               l.getIndex().equals(matchedProblem.getId().getIndex()));
                                               
                            if (!alreadySolved) {
                                SolveLog log = new SolveLog();
                                log.setHandle(handle);
                                log.setTeam(team.getName());
                                log.setTimestamp(subTime);
                                log.setContestId(matchedProblem.getId().getContestId());
                                log.setIndex(matchedProblem.getId().getIndex());
                                log.setMatch(match);
                                log.setProblem(matchedProblem);
                                
                                if (match.getSolveLogs() == null) match.setSolveLogs(new ArrayList<>());
                                match.getSolveLogs().add(log);
                                
                                newSolve = true;
                                
                                // REPLACE MODE LOGIC
                                if (match.getMode() == MatchMode.replace) {
                                    matchedProblem.setActive(false);
                                    
                                    int increment = match.getReplaceIncrement() != null ? match.getReplaceIncrement() : 100;
                                    int newRatingTarget = Math.min(3500, (matchedProblem.getRating() != null ? matchedProblem.getRating() : 0) + increment);
                                    
                                    List<com.cpclub.backend.codeforces.entity.CfProblem> pool = 
                                        cfProblemRepository.findRandomProblemsByRatingRange(
                                            newRatingTarget, 
                                            newRatingTarget, 
                                            10
                                        );
                                        
                                    // filter problems already in this match
                                    Set<String> existingKeys = match.getProblems().stream()
                                        .map(p -> p.getId().getContestId() + "-" + p.getId().getIndex())
                                        .collect(Collectors.toSet());
                                        
                                    com.cpclub.backend.codeforces.entity.CfProblem replacement = pool.stream()
                                        .filter(p -> p.getContestId() != null && !existingKeys.contains(p.getContestId() + "-" + p.getProblemIndex()))
                                        .findFirst().orElse(null);
                                        
                                    if (replacement != null) {
                                        Problem p = new Problem();
                                        ProblemId pid = new ProblemId(replacement.getContestId(), replacement.getProblemIndex(), matchId);
                                        p.setId(pid);
                                        p.setMatch(match);
                                        p.setName(replacement.getName());
                                        p.setRating(replacement.getRating());
                                        p.setPosition(matchedProblem.getPosition());
                                        p.setActive(true);
                                        match.getProblems().add(p);
                                        problemLookup.put(replacement.getContestId() + "-" + replacement.getProblemIndex(), p);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Failed to poll handle {}", handle, e);
            }
        }
        
        if (newSolve) {
            matchRepository.save(match);
        }
    }

    private MatchResponseDto mapToDto(Match match) {
        MatchResponseDto.MatchResponseDtoBuilder b = MatchResponseDto.builder()
            .id(match.getId())
            .startTime(match.getStartTime())
            .durationMinutes(match.getDurationMinutes())
            .mode(match.getMode())
            .replaceIncrement(match.getReplaceIncrement())
            .gridSize(match.getGridSize())
            .timeoutMinutes(match.getTimeoutMinutes())
            .showRatings(match.getShowRatings());
            
        List<MatchResponseDto.TeamDto> teams = match.getTeams().stream().map(t -> 
            MatchResponseDto.TeamDto.builder()
                .name(t.getName())
                .color(t.getColor())
                .members(t.getMembers().stream().map(Member::getHandle).toList())
                .build()
        ).toList();
        b.teams(teams);
        
        List<MatchResponseDto.SolveEntryDto> logs = (match.getSolveLogs() == null) ? List.of() : 
            match.getSolveLogs().stream().map(l -> 
                MatchResponseDto.SolveEntryDto.builder()
                    .handle(l.getHandle())
                    .team(l.getTeam())
                    .timestamp(l.getTimestamp())
                    .problem(MatchResponseDto.ProblemRefDto.builder()
                        .contestId(l.getContestId())
                        .index(l.getIndex())
                        .build())
                    .build()
            ).toList();
        b.solveLog(logs);
        
        List<MatchResponseDto.ProblemCellDto> probs = match.getProblems().stream().map(p -> {
            int pos = p.getPosition();
            int r = pos / match.getGridSize();
            int c = pos % match.getGridSize();
            
            String solvedBy = null;
            String claimedBy = null;
            if (match.getSolveLogs() != null) {
                Optional<SolveLog> log = match.getSolveLogs().stream()
                    .filter(l -> l.getContestId().equals(p.getId().getContestId()) && 
                                 l.getIndex().equals(p.getId().getIndex()))
                    .min(Comparator.comparing(SolveLog::getTimestamp));
                if (log.isPresent()) {
                    solvedBy = log.get().getTeam();
                    claimedBy = log.get().getHandle();
                }
            }
            
            return MatchResponseDto.ProblemCellDto.builder()
                .row(r)
                .col(c)
                .contestId(p.getId().getContestId())
                .index(p.getId().getIndex())
                .name(p.getName())
                .rating(p.getRating())
                .link("https://codeforces.com/contest/" + p.getId().getContestId() + "/problem/" + p.getId().getIndex())
                .active(p.getActive())
                .position(p.getPosition())
                .solvedBy(solvedBy)
                .claimedBy(claimedBy)
                .build();
        }).toList();
        b.problems(probs);
        
        return b.build();
    }
}
