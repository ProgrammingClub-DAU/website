package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Fallback source for AtCoder and CodeChef through clist.by (D26), used when an API key is set. */
@Slf4j
@Component
public class ClistContestSource {

    @Value("${cpclub.external-contests.clist-api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final JsonMapper json = JsonMapper.builder().build();

    public ClistContestSource(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<ExternalContestDto> fetchUpcoming(ContestPlatform platform) {
        if (!isConfigured()) return List.of();

        int resourceId = platform == ContestPlatform.ATCODER ? 93 : 2;
        String url = "https://clist.by/api/v4/contest/?resource_id=" + resourceId + "&upcoming=true&order_by=start";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "ApiKey " + apiKey);

        String raw;
        try {
            raw = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        } catch (Exception e) {
            // Never chain the exception: its message can quote the request.
            throw new IllegalStateException("clist.by request failed for " + platform);
        }
        JsonNode objects = raw == null ? null : json.readTree(raw).path("objects");
        if (objects == null || !objects.isArray()) {
            throw new IllegalStateException("clist.by returned no contest list for " + platform);
        }

        List<ExternalContestDto> upcoming = new ArrayList<>();
        for (JsonNode contest : objects) {
            String startStr = contest.path("start").asString("");
            if (startStr.isBlank()) continue;
            upcoming.add(ExternalContestDto.builder()
                    .id(contest.path("id").asString(""))
                    .platform(platform)
                    .name(contest.path("event").asString(""))
                    .url(contest.path("href").asString(""))
                    // clist returns UTC, which is what the column holds (D31).
                    .startsAt(LocalDateTime.parse(startStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .durationSeconds(contest.path("duration").asInt(0))
                    .build());
        }
        return upcoming;
    }
}
