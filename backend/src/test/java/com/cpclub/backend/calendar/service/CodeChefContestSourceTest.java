package com.cpclub.backend.calendar.service;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.service.sources.CodeChefContestSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CodeChefContestSourceTest {

    private static final String URL =
            "https://www.codechef.com/api/list/contests/all?sort_by=START&sorting_order=asc&offset=0&mode=all";

    private MockRestServiceServer server;
    private CodeChefContestSource source;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        source = new CodeChefContestSource(restTemplate);
    }

    /** CodeChef publishes IST times; the column holds UTC. It used to keep 20:00 as 20:00. */
    @Test
    void istStartIsConvertedToUtc() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"future_contests":[
                  {"contest_code":"START200","contest_name":"Starters 200",
                   "contest_start_date_iso":"2026-10-29T20:00:00+05:30","contest_duration":"120"}
                ]}""", MediaType.APPLICATION_JSON));

        List<ExternalContestDto> contests = source.fetchUpcoming();

        assertThat(contests).hasSize(1);
        assertThat(contests.get(0).getStartsAt()).isEqualTo(LocalDateTime.of(2026, 10, 29, 14, 30));
        assertThat(contests.get(0).getDurationSeconds()).isEqualTo(7200);
        assertThat(contests.get(0).getUrl()).isEqualTo("https://www.codechef.com/START200");
    }

    /** An empty list would delete every upcoming CodeChef contest; a bad reply must throw instead. */
    @Test
    void nonJsonReplyThrowsRatherThanReportingNoContests() {
        server.expect(requestTo(URL)).andRespond(withSuccess("<html>blocked</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> source.fetchUpcoming()).isInstanceOf(IllegalStateException.class);
    }
}
