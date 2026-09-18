package com.cpclub.backend.event.livesheet;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link SheetsApi} over Google's Sheets REST API, v4.
 *
 * <p>Everything a sync does to a tab goes in one {@code batchUpdate}, which
 * Google applies atomically. A sheet is therefore never seen half-written: it
 * holds either the previous list or the new one.</p>
 */
public class RestSheetsApi implements SheetsApi {

    private static final String BASE = "https://sheets.googleapis.com/v4/spreadsheets/";

    /** The cell fields a sync owns. Anything else an admin formats by hand is left alone. */
    private static final String CELL_FIELDS = "userEnteredValue,userEnteredFormat.textFormat.bold";

    private final RestTemplate restTemplate;
    private final GoogleAccessTokens tokens;

    public RestSheetsApi(RestTemplate restTemplate, GoogleAccessTokens tokens) {
        this.restTemplate = restTemplate;
        this.tokens = tokens;
    }

    @Override
    public List<SheetTab> listTabs(String spreadsheetId) {
        Map<?, ?> response = restTemplate.exchange(
                BASE + "{id}?fields=sheets.properties(sheetId,title,gridProperties(rowCount,columnCount))",
                HttpMethod.GET,
                new HttpEntity<>(headers()),
                Map.class,
                spreadsheetId
        ).getBody();

        List<SheetTab> tabs = new ArrayList<>();
        if (response != null && response.get("sheets") instanceof List<?> sheets) {
            for (Object sheet : sheets) {
                if (sheet instanceof Map<?, ?> s && s.get("properties") instanceof Map<?, ?> p) {
                    tabs.add(toTab(p));
                }
            }
        }
        return tabs;
    }

    @Override
    public SheetTab addTab(String spreadsheetId, String title, int rowCount, int columnCount) {
        Map<String, Object> addSheet = Map.of("addSheet", Map.of("properties", Map.of(
                "title", title,
                "gridProperties", Map.of(
                        "rowCount", rowCount,
                        "columnCount", columnCount,
                        "frozenRowCount", 1
                )
        )));

        Map<?, ?> response = batchUpdate(spreadsheetId, List.of(addSheet));

        // replies[0].addSheet.properties -- the only reply, for the only request.
        if (response != null
                && response.get("replies") instanceof List<?> replies
                && !replies.isEmpty()
                && replies.get(0) instanceof Map<?, ?> reply
                && reply.get("addSheet") instanceof Map<?, ?> added
                && added.get("properties") instanceof Map<?, ?> properties) {
            return toTab(properties);
        }
        throw new IllegalStateException("Google did not report the new tab.");
    }

    @Override
    public void replaceTab(String spreadsheetId, SheetTab tab, String title, List<List<String>> rows) {
        int width = rows.stream().mapToInt(List::size).max().orElse(0);
        List<Map<String, Object>> requests = new ArrayList<>();

        // 1. Room for every row and column. Writing past the grid is an error,
        //    and a new tab starts at whatever size addTab gave it.
        if (rows.size() > tab.rowCount()) {
            requests.add(appendDimension(tab.sheetId(), "ROWS", rows.size() - tab.rowCount()));
        }
        if (width > tab.columnCount()) {
            requests.add(appendDimension(tab.sheetId(), "COLUMNS", width - tab.columnCount()));
        }

        // 2. The tab's name follows the event's, and the header stays in view.
        requests.add(Map.of("updateSheetProperties", Map.of(
                "properties", Map.of(
                        "sheetId", tab.sheetId(),
                        "title", title,
                        "gridProperties", Map.of("frozenRowCount", 1)
                ),
                "fields", "title,gridProperties.frozenRowCount"
        )));

        // 3. Clear what the last sync wrote. An updateCells with a range and no
        //    rows clears the named fields across that range -- here, the whole tab.
        requests.add(Map.of("updateCells", Map.of(
                "range", Map.of("sheetId", tab.sheetId()),
                "fields", CELL_FIELDS
        )));

        // 4. Write the list, header in bold.
        requests.add(Map.of("updateCells", Map.of(
                "start", Map.of("sheetId", tab.sheetId(), "rowIndex", 0, "columnIndex", 0),
                "rows", toRowData(rows),
                "fields", CELL_FIELDS
        )));

        // 5. Fit the columns to what is in them.
        if (width > 0) {
            requests.add(Map.of("autoResizeDimensions", Map.of("dimensions", Map.of(
                    "sheetId", tab.sheetId(),
                    "dimension", "COLUMNS",
                    "startIndex", 0,
                    "endIndex", width
            ))));
        }

        batchUpdate(spreadsheetId, requests);
    }

    private Map<?, ?> batchUpdate(String spreadsheetId, List<Map<String, Object>> requests) {
        return restTemplate.exchange(
                BASE + "{id}:batchUpdate",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("requests", requests), headers()),
                Map.class,
                spreadsheetId
        ).getBody();
    }

    /**
     * Every value as a plain string.
     *
     * <p>{@code stringValue}, never a formula or a number: a member named
     * "=IMPORTXML(...)" is written as that text, not evaluated, and a student ID
     * with a leading zero keeps it.</p>
     */
    private static List<Map<String, Object>> toRowData(List<List<String>> rows) {
        List<Map<String, Object>> rowData = new ArrayList<>(rows.size());
        for (int r = 0; r < rows.size(); r++) {
            List<Map<String, Object>> cells = new ArrayList<>();
            for (String value : rows.get(r)) {
                Map<String, Object> cell = new LinkedHashMap<>();
                cell.put("userEnteredValue", Map.of("stringValue", value == null ? "" : value));
                if (r == 0) {
                    cell.put("userEnteredFormat", Map.of("textFormat", Map.of("bold", true)));
                }
                cells.add(cell);
            }
            rowData.add(Map.of("values", cells));
        }
        return rowData;
    }

    private static Map<String, Object> appendDimension(int sheetId, String dimension, int length) {
        return Map.of("appendDimension", Map.of(
                "sheetId", sheetId,
                "dimension", dimension,
                "length", length
        ));
    }

    private static SheetTab toTab(Map<?, ?> properties) {
        Map<?, ?> grid = properties.get("gridProperties") instanceof Map<?, ?> g ? g : Map.of();
        return new SheetTab(
                intOf(properties.get("sheetId")),
                properties.get("title") instanceof String t ? t : "",
                intOf(grid.get("rowCount")),
                intOf(grid.get("columnCount"))
        );
    }

    /** Google omits zero values, so a missing number is 0, not an error. */
    private static int intOf(Object value) {
        return value instanceof Number n ? n.intValue() : 0;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.get());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
