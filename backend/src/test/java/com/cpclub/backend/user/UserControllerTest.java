package com.cpclub.backend.user;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.GlobalExceptionHandler;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.controller.UserController;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

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
    @DisplayName("GET /api/users/all - Should return list of all users")
    void shouldGetAllUsers() throws Exception {
        UserResponseDto u1 = new UserResponseDto(1L, "Alice", "alice@example.com", "alice_cf", 1600, Role.ROLE_USER, LocalDateTime.now(), LocalDateTime.now());
        UserResponseDto u2 = new UserResponseDto(2L, "Bob", "bob@example.com", "bob_cf", 1800, Role.ROLE_ADMIN, LocalDateTime.now(), LocalDateTime.now());

        when(userService.getAllUsers()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", is("Alice")))
                .andExpect(jsonPath("$.data[1].name", is("Bob")));
    }

    @Test
    @DisplayName("GET /api/users - Should return members directory")
    void shouldGetMembersDirectory() throws Exception {
        UserResponseDto u1 = new UserResponseDto(1L, "Alice", "alice@example.com", "alice_cf", 1600, Role.ROLE_USER, LocalDateTime.now(), LocalDateTime.now());
        PagedResponse<UserResponseDto> paged = new PagedResponse<>(List.of(u1), 0, 20, 1L, 1, true);

        when(userService.getMembersDirectory(null, 0, 20)).thenReturn(paged);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].name", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return user when found")
    void shouldGetUserByIdWhenFound() throws Exception {
        UserResponseDto user = new UserResponseDto(5L, "Charlie", "charlie@example.com", "charlie_cf", 2000, Role.ROLE_USER, LocalDateTime.now(), LocalDateTime.now());

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
                .andExpect(jsonPath("$.message", containsString("User not found with id: 999")));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should update Codeforces handle")
    void shouldUpdateCodeforcesHandle() throws Exception {
        UserResponseDto updatedUser = new UserResponseDto(1L, "Alice", "alice@example.com", "new_cf_handle", 1600, Role.ROLE_USER, LocalDateTime.now(), LocalDateTime.now());

        when(userService.updateCodeforcesHandle(eq(1L), any(UpdateHandleRequest.class))).thenReturn(updatedUser);

        String jsonPayload = "{\"handle\":\"new_cf_handle\"}";

        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.codeforcesHandle", is("new_cf_handle")));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should return 400 Bad Request on blank handle")
    void shouldReturn400OnBlankHandle() throws Exception {
        String invalidJsonPayload = "{\"handle\":\"\"}";

        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }
}
