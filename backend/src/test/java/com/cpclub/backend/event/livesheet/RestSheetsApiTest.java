package com.cpclub.backend.event.livesheet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestSheetsApiTest {

    private static final String SPREADSHEET = "sheet-abc";
    private static final String BASE = "https://sheets.googleapis.com/v4/spreadsheets/" + SPREADSHEET;

    private MockRestServiceServer google;
    private RestSheetsApi api;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        google = MockRestServiceServer.bindTo(restTemplate).build();
        GoogleAccessTokens tokens = mock(GoogleAccessTokens.class);
        when(tokens.get()).thenReturn("tok-1");
        api = new RestSheetsApi(restTemplate, tokens);
    }

    @Test
    @DisplayName("Lists tabs, reading an omitted sheetId as 0 the way Google means it")
    void listsTabs() {
        google.expect(requestTo(startsWith(BASE + "?fields=")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer tok-1"))
                .andRespond(withSuccess("""
                        {"sheets":[
                          {"properties":{"title":"Sheet1","gridProperties":{"rowCount":1000,"columnCount":26}}},
                          {"properties":{"sheetId":42,"title":"#7 IPC","gridProperties":{"rowCount":100,"columnCount":6}}}
                        ]}""", MediaType.APPLICATION_JSON));

        List<SheetsApi.SheetTab> tabs = api.listTabs(SPREADSHEET);

        assertEquals(List.of(
                new SheetsApi.SheetTab(0, "Sheet1", 1000, 26),
                new SheetsApi.SheetTab(42, "#7 IPC", 100, 6)
        ), tabs);
        google.verify();
    }

    @Test
    @DisplayName("Adds a tab with a frozen header row and returns what Google created")
    void addsATab() {
        google.expect(requestTo(BASE + ":batchUpdate"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.requests[0].addSheet.properties.title").value("#7 IPC"))
                .andExpect(jsonPath("$.requests[0].addSheet.properties.gridProperties.frozenRowCount").value(1))
                .andRespond(withSuccess("""
                        {"replies":[{"addSheet":{"properties":
                          {"sheetId":99,"title":"#7 IPC","gridProperties":{"rowCount":100,"columnCount":6}}}}]}""",
                        MediaType.APPLICATION_JSON));

        SheetsApi.SheetTab tab = api.addTab(SPREADSHEET, "#7 IPC", 100, 6);

        assertEquals(new SheetsApi.SheetTab(99, "#7 IPC", 100, 6), tab);
        google.verify();
    }

    @Test
    @DisplayName("Rewrites a tab in one atomic request: grow, rename, clear, write, resize")
    void replacesATab() {
        SheetsApi.SheetTab tab = new SheetsApi.SheetTab(42, "#7 Old name", 2, 6);
        List<List<String>> rows = List.of(
                List.of("Name", "Codeforces Profile", "Email", "Student ID", "Year", "Added At"),
                List.of("Asha", "", "a@dau.ac.in", "a", "1st year", "2026-09-18 10:00"),
                List.of("=IMPORTXML(1)", "", "b@dau.ac.in", "b", "", "2026-09-18 10:01")
        );

        google.expect(requestTo(BASE + ":batchUpdate"))
                .andExpect(method(HttpMethod.POST))
                // Three rows into a tab with room for two: one row appended first.
                .andExpect(jsonPath("$.requests[0].appendDimension.dimension").value("ROWS"))
                .andExpect(jsonPath("$.requests[0].appendDimension.length").value(1))
                .andExpect(jsonPath("$.requests[1].updateSheetProperties.properties.title").value("#7 IPC 2026"))
                // Clear: a range with no rows.
                .andExpect(jsonPath("$.requests[2].updateCells.range.sheetId").value(42))
                .andExpect(jsonPath("$.requests[2].updateCells.rows").doesNotExist())
                // Write: header in bold, every value as plain text.
                .andExpect(jsonPath("$.requests[3].updateCells.rows[0].values[0].userEnteredValue.stringValue")
                        .value("Name"))
                .andExpect(jsonPath("$.requests[3].updateCells.rows[0].values[0].userEnteredFormat.textFormat.bold")
                        .value(true))
                .andExpect(jsonPath("$.requests[3].updateCells.rows[1].values[0].userEnteredFormat").doesNotExist())
                .andExpect(jsonPath("$.requests[3].updateCells.rows[2].values[0].userEnteredValue.stringValue")
                        .value("=IMPORTXML(1)"))
                .andExpect(jsonPath("$.requests[3].updateCells.rows[2].values[0].userEnteredValue.formulaValue")
                        .doesNotExist())
                .andExpect(jsonPath("$.requests[4].autoResizeDimensions.dimensions.endIndex").value(6))
                .andRespond(withSuccess("{\"replies\":[]}", MediaType.APPLICATION_JSON));

        api.replaceTab(SPREADSHEET, tab, "#7 IPC 2026", rows);

        google.verify();
    }
}
