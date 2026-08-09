package com.cpclub.backend.controller;

import com.cpclub.backend.dto.UpdateHandleRequest;
import com.cpclub.backend.dto.UserProfileUpdateRequest;
import com.cpclub.backend.dto.UserResponseDto;
import com.cpclub.backend.entity.Role;
import com.cpclub.backend.exception.GlobalExceptionHandler;
import com.cpclub.backend.exception.ResourceNotFoundException;
import com.cpclub.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        UserController userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/users - Should return list of all users wrapped in ApiResponse")
    void shouldGetAllUsers() throws Exception {
        UserResponseDto u1 = new UserResponseDto(1L, "Alice", "alice@example.com", "alice_cf", 1600, Role.ROLE_USER, LocalDateTime.now());
        UserResponseDto u2 = new UserResponseDto(2L, "Bob", "bob@example.com", "bob_cf", 1800, Role.ROLE_ADMIN, LocalDateTime.now());

        when(userService.getAllUsers()).thenReturn(Arrays.asList(u1, u2));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.statusCode", is(200)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", is("Alice")))
                .andExpect(jsonPath("$.data[1].name", is("Bob")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return user when found")
    void shouldGetUserByIdWhenFound() throws Exception {
        UserResponseDto user = new UserResponseDto(5L, "Charlie", "charlie@example.com", "charlie_cf", 2000, Role.ROLE_USER, LocalDateTime.now());

        when(userService.getUserById(5L)).thenReturn(user);

        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(5)))
                .andExpect(jsonPath("$.data.name", is("Charlie")))
                .andExpect(jsonPath("$.data.codeforcesHandle", is("charlie_cf")))
                .andExpect(jsonPath("$.data.rating", is(2000)));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return 404 Not Found when user does not exist")
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(404)))
                .andExpect(jsonPath("$.message", containsString("User not found with id: 999")));
    }

    @Test
    @DisplayName("GET /api/users/leaderboard - Should return users sorted by rating")
    void shouldGetLeaderboard() throws Exception {
        UserResponseDto topUser = new UserResponseDto(1L, "Top Coder", "top@example.com", "tourist", 3500, Role.ROLE_USER, LocalDateTime.now());

        when(userService.getLeaderboard()).thenReturn(Collections.singletonList(topUser));

        mockMvc.perform(get("/api/users/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].codeforcesHandle", is("tourist")))
                .andExpect(jsonPath("$.data[0].rating", is(3500)));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should update Codeforces handle")
    void shouldUpdateCodeforcesHandle() throws Exception {
        UserResponseDto updatedUser = new UserResponseDto(1L, "Alice", "alice@example.com", "new_cf_handle", 1600, Role.ROLE_USER, LocalDateTime.now());

        when(userService.updateCodeforcesHandle(eq(1L), any(UpdateHandleRequest.class))).thenReturn(updatedUser);

        String jsonPayload = "{\"codeforcesHandle\":\"new_cf_handle\"}";

        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.codeforcesHandle", is("new_cf_handle")));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should return 400 Bad Request on invalid handle format")
    void shouldReturn400OnInvalidHandleFormat() throws Exception {
        String invalidJsonPayload = "{\"codeforcesHandle\":\"a!\"}";

        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.statusCode", is(400)))
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/profile - Should update User profile name and handle")
    void shouldUpdateUserProfile() throws Exception {
        UserResponseDto updatedUser = new UserResponseDto(1L, "New Name", "alice@example.com", "valid_handle", 1600, Role.ROLE_USER, LocalDateTime.now());

        when(userService.updateUserProfile(eq(1L), any(UserProfileUpdateRequest.class))).thenReturn(updatedUser);

        String jsonPayload = "{\"name\":\"New Name\",\"codeforcesHandle\":\"valid_handle\"}";

        mockMvc.perform(put("/api/users/1/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("New Name")))
                .andExpect(jsonPath("$.data.codeforcesHandle", is("valid_handle")));
    }
}
