package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.Duration;

@Slf4j
@Component
public class AtCoderContestSource implements ExternalContestSource {

    private static final String ATCODER_URL = "https://atcoder.jp/contests/";

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.ATCODER;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        List<ExternalContestDto> upcoming = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(ATCODER_URL).get();
            Element upcomingDiv = doc.getElementById("contest-table-upcoming");
            if (upcomingDiv == null) return upcoming;
            
            Elements rows = upcomingDiv.select("tbody tr");
            for (Element row : rows) {
                Elements tds = row.select("td");
                if (tds.size() < 2) continue;
                
                String timeStr = tds.get(0).select("time").text();
                // Format: 2026-09-20 21:00:00+0900
                ZonedDateTime startsAtZoned = ZonedDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ"));
                
                Element link = tds.get(1).select("a").first();
                String url = link.attr("href");
                if (!url.startsWith("http")) url = "https://atcoder.jp" + url;
                String name = link.text();
                String id = url.substring(url.lastIndexOf("/") + 1);
                
                String durationStr = tds.get(2).text(); // Format: 01:40
                String[] parts = durationStr.split(":");
                int durationSeconds = 0;
                if (parts.length == 2) {
                    durationSeconds = Integer.parseInt(parts[0]) * 3600 + Integer.parseInt(parts[1]) * 60;
                }
                
                upcoming.add(ExternalContestDto.builder()
                        .id(id)
                        .platform(ContestPlatform.ATCODER)
                        .name(name)
                        .url(url)
                        .startsAt(startsAtZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                        .durationSeconds(durationSeconds)
                        .build());
            }
        } catch (IOException e) {
            log.error("Failed to fetch AtCoder contests", e);
            throw new RuntimeException("Failed to fetch AtCoder contests", e);
        }
        return upcoming;
    }
}
