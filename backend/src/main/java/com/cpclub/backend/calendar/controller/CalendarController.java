package com.cpclub.backend.calendar.controller;

import com.cpclub.backend.calendar.dto.CalendarItemDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.service.CalendarService;
import com.cpclub.backend.calendar.service.IcsWriter;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final IcsWriter icsWriter;
    private final EventRepository eventRepository;

    @Value("${cpclub.public-site-url:http://localhost:3000}")
    private String publicSiteUrl;

    private String getHost() {
        try {
            return new URI(publicSiteUrl).getHost();
        } catch (Exception e) {
            return "localhost";
        }
    }

    @GetMapping("/calendar")
    public List<CalendarItemDto> getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) List<ContestPlatform> platforms) {
        try {
            return calendarService.getRange(from, to, platforms);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/calendar/club.ics")
    public ResponseEntity<String> getClubIcs(
            @RequestParam(required = false, defaultValue = "events,contests") List<String> include,
            @RequestParam(required = false) List<ContestPlatform> platforms) {
        
        // 90 days from now, or some range? The prompt doesn't specify a range for the feed. Let's do next 30 days.
        LocalDateTime from = LocalDateTime.now(ZoneOffset.UTC).minusDays(7);
        LocalDateTime to = from.plusDays(60);
        
        List<CalendarItemDto> items = calendarService.getRange(from, to, platforms);
        List<IcsWriter.IcsEvent> icsEvents = new ArrayList<>();
        String host = getHost();

        for (CalendarItemDto item : items) {
            if (item.getType().equals("CLUB_EVENT") && !include.contains("events")) continue;
            if (item.getType().equals("EXTERNAL_CONTEST") && !include.contains("contests")) continue;

            IcsWriter.IcsEvent event = new IcsWriter.IcsEvent();
            if (item.getType().equals("CLUB_EVENT")) {
                event.setUid("event-" + item.getId() + "@" + host);
                event.setUrl(publicSiteUrl + item.getUrl());
            } else {
                event.setUid(item.getPlatform().toLowerCase() + "-" + item.getId() + "@" + host);
                event.setUrl(item.getUrl());
            }
            event.setStartUtc(item.getStartsAt());
            event.setEndUtc(item.getEndsAt());
            event.setSummary(item.getTitle());
            icsEvents.add(event);
        }

        String icsData = icsWriter.write(icsEvents);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        headers.setCacheControl("public, max-age=900");

        return new ResponseEntity<>(icsData, headers, HttpStatus.OK);
    }

    @GetMapping("/events/{id}/ics")
    public ResponseEntity<String> getEventIcs(@PathVariable Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event is cancelled");
        }

        IcsWriter.IcsEvent icsEvent = new IcsWriter.IcsEvent();
        icsEvent.setUid("event-" + event.getId() + "@" + getHost());
        icsEvent.setStartUtc(event.getEventDate().atZone(java.time.ZoneId.of("Asia/Kolkata")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        // Default end time since event entity only has start time
        icsEvent.setEndUtc(icsEvent.getStartUtc().plusHours(2));
        icsEvent.setSummary(event.getTitle());
        icsEvent.setDescription(event.getDescription());
        icsEvent.setUrl(publicSiteUrl + "/events/" + event.getId());

        String icsData = icsWriter.write(List.of(icsEvent));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/calendar"));
        
        return new ResponseEntity<>(icsData, headers, HttpStatus.OK);
    }
}
