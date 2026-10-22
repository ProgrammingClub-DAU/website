package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Upcoming AtCoder contests, scraped from the public contest list (no API exists). */
@Slf4j
@Component
public class AtCoderContestSource implements ExternalContestSource {

    private static final String ATCODER_URL = "https://atcoder.jp/contests/";
    private static final DateTimeFormatter START_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ");
    private static final int TIMEOUT_MS = 15_000;

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.ATCODER;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        Document doc;
        try {
            doc = Jsoup.connect(ATCODER_URL)
                    .userAgent(LeetCodeSyncService.BROWSER_USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch AtCoder contests: " + e.getMessage(), e);
        }

        Element upcomingDiv = doc.getElementById("contest-table-upcoming");
        if (upcomingDiv == null) {
            // AtCoder drops the table when nothing is scheduled; anything else is a layout change.
            if (doc.getElementById("contest-table-active") == null && doc.getElementById("contest-table-recent") == null) {
                throw new IllegalStateException("AtCoder page layout changed: contest tables not found");
            }
            return List.of();
        }

        List<ExternalContestDto> upcoming = new ArrayList<>();
        for (Element row : upcomingDiv.select("tbody tr")) {
            Elements tds = row.select("td");
            if (tds.size() < 3) continue;

            Element link = tds.get(1).select("a").first();
            if (link == null) continue;

            ZonedDateTime startsAt;
            try {
                // Format: 2026-09-20 21:00:00+0900
                startsAt = ZonedDateTime.parse(tds.get(0).select("time").text(), START_FORMAT);
            } catch (DateTimeParseException e) {
                log.warn("Skipping AtCoder row with unreadable start time: {}", tds.get(0).text());
                continue;
            }

            String url = link.attr("href");
            if (!url.startsWith("http")) url = "https://atcoder.jp" + url;
            String id = url.substring(url.lastIndexOf('/') + 1);

            upcoming.add(ExternalContestDto.builder()
                    .id(id)
                    .platform(ContestPlatform.ATCODER)
                    .name(link.text())
                    .url(url)
                    .startsAt(startsAt.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                    .durationSeconds(parseDuration(tds.get(2).text()))
                    .build());
        }
        return upcoming;
    }

    /** "01:40", or "240:00" for long contests. */
    static int parseDuration(String text) {
        String[] parts = text.trim().split(":");
        if (parts.length != 2) return 0;
        try {
            return Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
