package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CodeforcesContestSource implements ExternalContestSource {

    private final CodeforcesApiClient apiClient;

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.CODEFORCES;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        List<JsonNode> contests = apiClient.contestList(false);
        List<ExternalContestDto> upcoming = new ArrayList<>();
        
        for (JsonNode contest : contests) {
            String phase = contest.path("phase").asText();
            if ("BEFORE".equals(phase)) {
                long id = contest.path("id").asLong();
                String name = contest.path("name").asText();
                long startTimeSeconds = contest.path("startTimeSeconds").asLong();
                int durationSeconds = contest.path("durationSeconds").asInt();
                
                upcoming.add(ExternalContestDto.builder()
                        .id(String.valueOf(id))
                        .platform(ContestPlatform.CODEFORCES)
                        .name(name)
                        .url("https://codeforces.com/contest/" + id)
                        .startsAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(startTimeSeconds), ZoneOffset.UTC))
                        .durationSeconds(durationSeconds)
                        .build());
            }
        }
        
        return upcoming;
    }
}
