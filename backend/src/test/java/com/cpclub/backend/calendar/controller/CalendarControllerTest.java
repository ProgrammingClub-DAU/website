package com.cpclub.backend.calendar.controller;

import com.cpclub.backend.calendar.service.CalendarService;
import com.cpclub.backend.calendar.service.IcsWriter;
import com.cpclub.backend.event.entity.Event;
import com.cpclub.backend.event.entity.EventStatus;
import com.cpclub.backend.event.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CalendarControllerTest {

    private MockMvc mockMvc;
    private CalendarService calendarService;
    private IcsWriter icsWriter;
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() {
        calendarService = mock(CalendarService.class);
        icsWriter = mock(IcsWriter.class);
        eventRepository = mock(EventRepository.class);
        
        CalendarController controller = new CalendarController(calendarService, icsWriter, eventRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void when90DayRange_returns400() throws Exception {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(90);

        when(calendarService.getRange(any(), any(), any())).thenThrow(new IllegalArgumentException("Date range cannot exceed 62 days"));

        mockMvc.perform(get("/api/calendar")
                .param("from", from.format(DateTimeFormatter.ISO_DATE_TIME))
                .param("to", to.format(DateTimeFormatter.ISO_DATE_TIME)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenCancelledClubEvent_notReturned() throws Exception {
        Event cancelledEvent = new Event();
        cancelledEvent.setId(1L);
        cancelledEvent.setStatus(EventStatus.CANCELLED);
        
        when(eventRepository.findById(1L)).thenReturn(Optional.of(cancelledEvent));
        
        mockMvc.perform(get("/api/events/1/ics"))
                .andExpect(status().isNotFound());
    }
}
