package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

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

    private final RestTemplate restTemplate;
    private final JsonMapper json = JsonMapper.builder().build();
    private final RateLimiter rateLimiter;

    public LeetCodeContestSource(RestTemplate restTemplate,
                                 @Qualifier("leetcodeRateLimiter") RateLimiter rateLimiter) {
        this.restTemplate = restTemplate;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.LEETCODE;
    }

    /**
     * Throws when LeetCode does not answer usefully.
     *
     * <p>It used to log and return an empty list, which the sync reads as "LeetCode
     * has no upcoming contests" -- and deletes every future LeetCode contest from the
     * calendar whenever LeetCode had a bad minute.</p>
     */
    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        rateLimiter.acquire();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.REFERER, "https://leetcode.com");
        headers.set(HttpHeaders.USER_AGENT, LeetCodeSyncService.BROWSER_USER_AGENT);

        String jsonString = restTemplate.postForObject(
                LEETCODE_GRAPHQL_URL,
                new HttpEntity<>(Map.of("query", UPCOMING_CONTESTS_QUERY), headers),
                String.class
        );
        if (jsonString == null || jsonString.isBlank()) {
            throw new IllegalStateException("LeetCode returned an empty response");
        }

        JsonNode contests = json.readTree(jsonString).path("data").path("upcomingContests");
        if (!contests.isArray()) {
            throw new IllegalStateException("LeetCode response had no upcomingContests list");
        }

        List<ExternalContestDto> upcoming = new ArrayList<>();
        for (JsonNode contest : contests) {
            String titleSlug = contest.path("titleSlug").asString("");
            if (titleSlug.isBlank()) continue;
            long startTime = contest.path("startTime").asLong(0L);

            upcoming.add(ExternalContestDto.builder()
                    .id(titleSlug)
                    .platform(ContestPlatform.LEETCODE)
                    .name(contest.path("title").asString(titleSlug))
                    .url("https://leetcode.com/contest/" + titleSlug)
                    .startsAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(startTime), ZoneOffset.UTC))
                    .durationSeconds(contest.path("duration").asInt(0))
                    .build());
        }
        return upcoming;
    }
}
