package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CodeChefContestSource implements ExternalContestSource {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.CODECHEF;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        List<ExternalContestDto> upcoming = new ArrayList<>();
        try {
            JsonNode response = restTemplate.getForObject("https://www.codechef.com/api/list/contests/all?sort_by=START&sorting_order=asc&offset=0&mode=premium", JsonNode.class);
            if (response != null && response.has("future_contests")) {
                for (JsonNode contest : response.get("future_contests")) {
                    String id = contest.path("contest_code").asText();
                    String name = contest.path("contest_name").asText();
                    String startIso = contest.path("contest_start_date_iso").asText();
                    int durationMinutes = contest.path("contest_duration").asInt();
                    
                    upcoming.add(ExternalContestDto.builder()
                            .id(id)
                            .platform(ContestPlatform.CODECHEF)
                            .name(name)
                            .url("https://www.codechef.com/" + id)
                            .startsAt(LocalDateTime.parse(startIso, DateTimeFormatter.ISO_OFFSET_DATE_TIME).atOffset(ZoneOffset.UTC).toLocalDateTime())
                            .durationSeconds(durationMinutes * 60)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch CodeChef contests", e);
            throw new RuntimeException("Failed to fetch CodeChef contests", e);
        }
        return upcoming;
    }
}
