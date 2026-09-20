package com.cpclub.backend.compete.controller;

import com.cpclub.backend.compete.dto.MatchCreationDto;
import com.cpclub.backend.compete.service.CompeteMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/compete/matches")
@RequiredArgsConstructor
public class CompeteController {

    private final CompeteMatchService competeMatchService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createMatch(@RequestBody MatchCreationDto dto) {
        return ResponseEntity.ok(competeMatchService.createMatch(dto));
    }

    @GetMapping("/{matchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMatch(@PathVariable String matchId) {
        return ResponseEntity.ok(competeMatchService.getMatch(matchId));
    }

    @PostMapping("/{matchId}/poll")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> pollMatch(@PathVariable String matchId) {
        competeMatchService.pollMatch(matchId);
        return ResponseEntity.ok(competeMatchService.getMatch(matchId));
    }

    // Bug #10 fix: propagate winner/match-end to all clients via next poll
    @PatchMapping("/{matchId}/duration")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> setDuration(@PathVariable String matchId, @RequestBody Map<String, Integer> body) {
        int duration = body.getOrDefault("durationMinutes", 1);
        competeMatchService.setMatchDuration(matchId, duration);
        return ResponseEntity.ok(Map.of("durationMinutes", duration));
    }
}

