package com.cpclub.backend.codeforces;

import com.cpclub.backend.codeforces.controller.CodeforcesController;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CodeforcesControllerTest {

    private MockMvc mockMvc;
    private CodeforcesSyncService syncService;

    @BeforeEach
    void setUp() {
        syncService = mock(CodeforcesSyncService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CodeforcesController(syncService)).build();
    }

    @Test
    void triggerSync_runsManualSynchronization() throws Exception {
        doNothing().when(syncService).syncCodeforcesRatings();

        mockMvc.perform(post("/api/codeforces/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Codeforces synchronization triggered successfully"));

        verify(syncService).syncCodeforcesRatings();
    }
}
