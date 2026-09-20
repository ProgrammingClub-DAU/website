package com.cpclub.backend.calendar.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Component
public class IcsWriter {

    private static final DateTimeFormatter UTC_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public String write(List<IcsEvent> events) {
        StringBuilder sb = new StringBuilder();
        appendLine(sb, "BEGIN:VCALENDAR");
        appendLine(sb, "VERSION:2.0");
        appendLine(sb, "PRODID:-//Programming Club DAU//Calendar//EN");
        appendLine(sb, "CALSCALE:GREGORIAN");

        for (IcsEvent event : events) {
            appendLine(sb, "BEGIN:VEVENT");
            appendLine(sb, "UID:" + event.getUid());
            appendLine(sb, "DTSTAMP:" + UTC_FORMATTER.format(LocalDateTime.now(ZoneOffset.UTC)));
            appendLine(sb, "DTSTART:" + UTC_FORMATTER.format(event.getStartUtc()));
            if (event.getEndUtc() != null) {
                appendLine(sb, "DTEND:" + UTC_FORMATTER.format(event.getEndUtc()));
            }
            appendLine(sb, "SUMMARY:" + escape(event.getSummary()));
            if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                appendLine(sb, "DESCRIPTION:" + escape(event.getDescription()));
            }
            if (event.getUrl() != null && !event.getUrl().isEmpty()) {
                appendLine(sb, "URL:" + event.getUrl());
            }
            appendLine(sb, "END:VEVENT");
        }

        appendLine(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private String escape(String text) {
        return text.replace("\\", "\\\\")
                   .replace(";", "\\;")
                   .replace(",", "\\,")
                   .replace("\n", "\\n")
                   .replace("\r", "");
    }

    private void appendLine(StringBuilder sb, String line) {
        // Line folding at 75 octets
        byte[] bytes = line.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int offset = 0;
        boolean first = true;
        while (offset < bytes.length) {
            int length = Math.min(bytes.length - offset, first ? 75 : 74);
            // Ensure we don't split a multi-byte character
            while (length > 0 && (bytes[offset + length - 1] & 0xC0) == 0x80) {
                length--;
            }
            if (length == 0) {
                length = 1; // Fallback to avoid infinite loop
            }
            
            if (!first) {
                sb.append(" ");
            }
            sb.append(new String(bytes, offset, length, java.nio.charset.StandardCharsets.UTF_8));
            sb.append("\r\n");
            
            offset += length;
            first = false;
        }
    }

    public static class IcsEvent {
        private String uid;
        private LocalDateTime startUtc;
        private LocalDateTime endUtc;
        private String summary;
        private String description;
        private String url;

        // getters and setters
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        public LocalDateTime getStartUtc() { return startUtc; }
        public void setStartUtc(LocalDateTime startUtc) { this.startUtc = startUtc; }
        public LocalDateTime getEndUtc() { return endUtc; }
        public void setEndUtc(LocalDateTime endUtc) { this.endUtc = endUtc; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
