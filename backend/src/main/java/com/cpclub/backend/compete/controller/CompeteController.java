package com.cpclub.backend.compete.controller;

import com.cpclub.backend.compete.service.CompeteMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compete/matches")
@RequiredArgsConstructor
public class CompeteController {

    private final CompeteMatchService competeMatchService;

    @PostMapping
    public ResponseEntity<?> createMatch() {
        return ResponseEntity.ok(competeMatchService.createMatch());
    }

    @PostMapping("/{matchId}/poll")
    public ResponseEntity<?> pollMatch(@PathVariable String matchId) {
        competeMatchService.pollMatch(matchId);
        return ResponseEntity.ok().build();
    }
}
