package com.cpclub.backend.compete.service;

import com.cpclub.backend.compete.entity.Match;
import com.cpclub.backend.compete.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompeteMatchService {

    private final MatchRepository matchRepository;

    public Match getMatch(String matchId) {
        return null;
    }

    public Match createMatch() {
        // Placeholder
        return null;
    }

    public void pollMatch(String matchId) {
        // Placeholder
    }
}
