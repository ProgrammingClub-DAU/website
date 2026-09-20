package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ClistContestSource {

    @Value("${cpclub.external-contests.clist-api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<ExternalContestDto> fetchUpcoming(ContestPlatform platform) {
        if (!isConfigured()) return List.of();
        
        List<ExternalContestDto> upcoming = new ArrayList<>();
        try {
            int resourceId = platform == ContestPlatform.ATCODER ? 93 : 2; // Approximate IDs for clist
            String url = "https://clist.by/api/v4/contest/?resource_id=" + resourceId + "&upcoming=true&order_by=start";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "ApiKey " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            JsonNode response = restTemplate.exchange(url, HttpMethod.GET, entity, JsonNode.class).getBody();
            if (response != null && response.has("objects")) {
                for (JsonNode contest : response.get("objects")) {
                    String id = contest.path("id").asText();
                    String name = contest.path("event").asText();
                    String startStr = contest.path("start").asText();
                    int durationSeconds = contest.path("duration").asInt();
                    String link = contest.path("href").asText();
                    
                    upcoming.add(ExternalContestDto.builder()
                            .id(id)
                            .platform(platform)
                            .name(name)
                            .url(link)
                            .startsAt(LocalDateTime.parse(startStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")))
                            .durationSeconds(durationSeconds)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch Clist contests for platform " + platform, e);
            throw new RuntimeException("Failed to fetch Clist contests", e);
        }
        return upcoming;
    }
}
