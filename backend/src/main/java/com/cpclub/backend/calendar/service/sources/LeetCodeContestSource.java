package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LeetCodeContestSource implements ExternalContestSource {

    private static final String LEETCODE_GRAPHQL_URL = "https://leetcode.com/graphql";
    private static final String UPCOMING_CONTESTS_QUERY = """
            query upcomingContests {
                upcomingContests {
                    title
                    titleSlug
                    startTime
                    duration
                }
            }
            """;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final RateLimiter rateLimiter;

    public LeetCodeContestSource(@Qualifier("leetcodeRateLimiter") RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.LEETCODE;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        rateLimiter.acquire();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.REFERER, "https://leetcode.com");
        headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        Map<String, Object> body = Map.of(
                "query", UPCOMING_CONTESTS_QUERY
        );

        List<ExternalContestDto> upcoming = new ArrayList<>();
        try {
            String jsonString = restTemplate.postForObject(
                    LEETCODE_GRAPHQL_URL,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            if (jsonString == null || jsonString.isBlank()) {
                return upcoming;
            }

            JsonNode response = OBJECT_MAPPER.readTree(jsonString);
            if (response != null && response.has("data") && response.get("data").has("upcomingContests")) {
                for (JsonNode contest : response.get("data").get("upcomingContests")) {
                    long startTime = contest.path("startTime").asLong();
                    int duration = contest.path("duration").asInt();
                    String title = contest.path("title").asText();
                    String titleSlug = contest.path("titleSlug").asText();
                    
                    upcoming.add(ExternalContestDto.builder()
                            .id(titleSlug)
                            .platform(ContestPlatform.LEETCODE)
                            .name(title)
                            .url("https://leetcode.com/contest/" + titleSlug)
                            .startsAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneOffset.UTC))
                            .durationSeconds(duration)
                            .build());
                }
            } else {
                log.warn("LeetCode GraphQL response did not contain upcomingContests: {}", response);
            }
        } catch (Exception e) {
            log.error("Failed to fetch upcoming LeetCode contests: {}", e.getMessage());
        }

        return upcoming;
    }
}
