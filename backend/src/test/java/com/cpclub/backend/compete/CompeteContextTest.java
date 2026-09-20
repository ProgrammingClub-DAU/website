package com.cpclub.backend.compete;

import com.cpclub.backend.codeforces.entity.CfProblem;
import com.cpclub.backend.codeforces.repository.CfProblemRepository;
import com.cpclub.backend.compete.dto.MatchCreationDto;
import com.cpclub.backend.compete.dto.MatchResponseDto;
import com.cpclub.backend.compete.entity.MatchMode;
import com.cpclub.backend.compete.service.CompeteMatchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CompeteContextTest {

    @Autowired
    private CompeteMatchService competeMatchService;

    @Autowired
    private CfProblemRepository cfProblemRepository;

    @Test
    void contextLoads() {
        // Validates JPA entities and Flyway migrations
    }

    @Test
    @Transactional
    void testCreateMatchEndToEnd() {
        // Seed test problems
        for (int i = 1; i <= 20; i++) {
            cfProblemRepository.save(CfProblem.builder()
                .problemKey("100" + i + "/A")
                .contestId(1000 + i)
                .problemIndex("A")
                .name("Problem " + i)
                .rating(1200)
                .solvedCount(500)
                .build());
        }

        MatchCreationDto.TeamDto team1 = new MatchCreationDto.TeamDto();
        team1.setName("Team Red");
        team1.setColor("red");
        team1.setMembers(List.of("tourist"));

        MatchCreationDto.TeamDto team2 = new MatchCreationDto.TeamDto();
        team2.setName("Team Blue");
        team2.setColor("blue");
        team2.setMembers(List.of("petr"));

        MatchCreationDto dto = new MatchCreationDto();
        dto.setMode(MatchMode.classic);
        dto.setStartTime(Instant.now().plusSeconds(300));
        dto.setDurationMinutes(60);
        dto.setMinRating(1000);
        dto.setMaxRating(1400);
        dto.setGridSize(3); // 3x3 = 9 problems
        dto.setShowRatings(true);
        dto.setTeams(List.of(team1, team2));

        MatchResponseDto res = competeMatchService.createMatch(dto);

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals(3, res.getGridSize());
        assertEquals(9, res.getProblems().size());
        assertEquals(2, res.getTeams().size());
    }
}
