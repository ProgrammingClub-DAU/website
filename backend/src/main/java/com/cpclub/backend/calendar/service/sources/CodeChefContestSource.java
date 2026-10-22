package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Upcoming CodeChef contests from CodeChef's own (unofficial) list endpoint.
 *
 * <p>Three bugs lived here. The response was requested as a Jackson 2
 * {@code JsonNode}, which Spring Boot 4's Jackson 3 converter cannot build, so
 * every run failed with "Failed to fetch CodeChef contests". The start time was
 * parsed as a {@code LocalDateTime} from an offset timestamp -- dropping the
 * {@code +05:30} -- and then labelled UTC, putting every contest five and a half
 * hours late. And the request went out with no timeout.</p>
 */
@Slf4j
@Component
public class CodeChefContestSource implements ExternalContestSource {

    private static final String URL =
            "https://www.codechef.com/api/list/contests/all?sort_by=START&sorting_order=asc&offset=0&mode=all";
    private static final ZoneId CODECHEF_ZONE = ZoneId.of("Asia/Kolkata");
    /** {@code contest_start_date}, used when the ISO field is missing: "23 Oct 2026  20:00:00". */
    private static final DateTimeFormatter LEGACY_START =
            DateTimeFormatter.ofPattern("d MMM yyyy[  ][ ]HH:mm:ss", Locale.ENGLISH);

    private final RestTemplate restTemplate;
    private final JsonMapper json = JsonMapper.builder().build();

    public CodeChefContestSource(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.CODECHEF;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set(HttpHeaders.USER_AGENT, LeetCodeSyncService.BROWSER_USER_AGENT);

        String raw;
        try {
            raw = restTemplate.exchange(URL, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
        } catch (Exception e) {
            throw new IllegalStateException("CodeChef request failed: " + e.getMessage(), e);
        }
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("CodeChef returned an empty response");
        }

        JsonNode response;
        try {
            response = json.readTree(raw);
        } catch (Exception e) {
            // An HTML block page from CodeChef's edge rather than JSON.
            throw new IllegalStateException("CodeChef returned a non-JSON response", e);
        }
        JsonNode future = response.path("future_contests");
        if (!future.isArray()) {
            throw new IllegalStateException("CodeChef response had no future_contests list");
        }

        List<ExternalContestDto> upcoming = new ArrayList<>();
        for (JsonNode contest : future) {
            String id = contest.path("contest_code").asString("");
            if (id.isBlank()) continue;
            LocalDateTime startsAtUtc = parseStart(contest);
            if (startsAtUtc == null) {
                log.warn("Skipping CodeChef contest {}: unreadable start time", id);
                continue;
            }
            int durationMinutes = contest.path("contest_duration").asInt(0);

            upcoming.add(ExternalContestDto.builder()
                    .id(id)
                    .platform(ContestPlatform.CODECHEF)
                    .name(contest.path("contest_name").asString(id))
                    .url("https://www.codechef.com/" + id)
                    .startsAt(startsAtUtc)
                    .durationSeconds(durationMinutes * 60)
                    .build());
        }
        return upcoming;
    }

    /** Start time as UTC wall-clock, per D31. */
    static LocalDateTime parseStart(JsonNode contest) {
        String iso = contest.path("contest_start_date_iso").asString("");
        if (!iso.isBlank()) {
            try {
                return OffsetDateTime.parse(iso).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                // fall through to the legacy field
            }
        }
        String legacy = contest.path("contest_start_date").asString("");
        if (!legacy.isBlank()) {
            try {
                return LocalDateTime.parse(legacy.trim(), LEGACY_START)
                        .atZone(CODECHEF_ZONE)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime();
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
        return null;
    }
}
