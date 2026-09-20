package com.cpclub.backend.calendar.service;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IcsWriterTest {

    @Test
    void testSummaryLineFoldingAndEscaping() {
        IcsWriter writer = new IcsWriter();
        IcsWriter.IcsEvent event = new IcsWriter.IcsEvent();
        event.setUid("test-uid@localhost");
        event.setStartUtc(LocalDateTime.of(2026, 9, 20, 10, 0, 0));
        
        // 120 chars with comma and newline
        String summary = "This is a very long summary that exceeds seventy five octets, and it also contains a newline\ncharacter to test escaping.";
        event.setSummary(summary);
        
        String output = writer.write(List.of(event));
        
        assertTrue(output.contains("SUMMARY:This is a very long summary that exceeds seventy five octets\\, and"));
        assertTrue(output.contains("\r\n it also contains a newline\\ncharacter to test escaping."));
    }
}
