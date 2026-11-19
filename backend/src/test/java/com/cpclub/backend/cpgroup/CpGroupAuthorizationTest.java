package com.cpclub.backend.cpgroup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The CP Practice Group is for admins only: a student cannot read or change it. */
@SpringBootTest
@ActiveProfiles("test")
class CpGroupAuthorizationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("A student gets 403 on every group endpoint")
    void student_isForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/cp-group/members")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/cp-group/members/1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/cp-group/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userIds\":[1],\"batchYear\":2029}")).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/admin/cp-group/members/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"notes\":\"x\"}")).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/cp-group/members/1")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Signed out: 401, including for one's own membership")
    void anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/cp-group/members")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/cp-group/me")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("An admin can list the group, and bad input is a 400")
    void admin_canList() throws Exception {
        mockMvc.perform(get("/api/admin/cp-group/members")).andExpect(status().isOk());
        mockMvc.perform(post("/api/admin/cp-group/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userIds\":[],\"batchYear\":2029}")).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/cp-group/members").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userIds\":[1],\"batchYear\":29}")).andExpect(status().isBadRequest());
    }
}
