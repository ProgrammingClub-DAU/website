package com.cpclub.backend.event.livesheet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiveSheetConfigTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    @DisplayName("Neither variable set: off, with no problem to report")
    void offWhenUnset() {
        AttendanceLiveSheet.Setup setup = LiveSheetConfig.setup("", "", restTemplate, objectMapper);

        assertFalse(setup.enabled());
        assertNull(setup.problem());
    }

    @Test
    @DisplayName("Only one variable set: off, and the missing one is named")
    void namesTheMissingVariable() {
        AttendanceLiveSheet.Setup noKey = LiveSheetConfig.setup("abc", "", restTemplate, objectMapper);
        AttendanceLiveSheet.Setup noId = LiveSheetConfig.setup("", "{}", restTemplate, objectMapper);

        assertTrue(noKey.problem().contains("GOOGLE_SERVICE_ACCOUNT_JSON"));
        assertTrue(noId.problem().contains("ATTENDANCE_SPREADSHEET_ID"));
    }

    @Test
    @DisplayName("A bad key does not stop the server; it becomes a problem on the status")
    void badKeyIsAProblemNotACrash() {
        AttendanceLiveSheet.Setup setup = LiveSheetConfig.setup("abc", "{\"client_email\":\"x\"}",
                restTemplate, objectMapper);

        assertFalse(setup.enabled());
        assertTrue(setup.problem().contains("private_key"));
    }

    @Test
    @DisplayName("Both set and valid: on, writing as the service account")
    void onWhenValid() {
        AttendanceLiveSheet.Setup setup = LiveSheetConfig.setup("abc", new TestServiceAccount().json(),
                restTemplate, objectMapper);

        assertTrue(setup.enabled());
        assertNotNull(setup.api());
        assertEquals(TestServiceAccount.EMAIL, setup.serviceAccountEmail());
    }

    @Test
    @DisplayName("The spreadsheet can be given as its id or as the URL from the address bar")
    void acceptsTheUrl() {
        String id = "1AbC_dEf-123";

        assertEquals(id, LiveSheetConfig.spreadsheetIdFrom(id));
        assertEquals(id, LiveSheetConfig.spreadsheetIdFrom(
                "https://docs.google.com/spreadsheets/d/" + id + "/edit#gid=0"));
    }
}
