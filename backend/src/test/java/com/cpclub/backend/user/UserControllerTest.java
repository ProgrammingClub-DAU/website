package com.cpclub.backend.user;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.GlobalExceptionHandler;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.controller.UserController;
import com.cpclub.backend.user.dto.PublicUserResponseDto;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
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
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        // Prevent SecurityContext from leaking between tests.
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper: sets a mock authenticated user in SecurityContextHolder so
     * @AuthenticationPrincipal is populated in standaloneSetup tests.
     */
    private void authenticateAs(String email, String role) {
        var userDetails = User.withUsername(email)
                .password("irrelevant")
                .authorities(new SimpleGrantedAuthority(role))
                .build();
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/users/all - Should return list of all users")
    void shouldGetAllUsers() throws Exception {
        UserResponseDto u1 = userDto(1L, "Alice", "alice@example.com", "alice_cf", 1600, Role.ROLE_USER);
        UserResponseDto u2 = userDto(2L, "Bob", "bob@example.com", "bob_cf", 1800, Role.ROLE_ADMIN);

        when(userService.getAllUsers()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name", is("Alice")))
                .andExpect(jsonPath("$.data[1].name", is("Bob")));
    }

    @Test
    @DisplayName("GET /api/users - Should return members directory (email-safe)")
    void shouldGetMembersDirectory() throws Exception {
        PublicUserResponseDto u1 = new PublicUserResponseDto(1L, "Alice", "alice_cf", 1600, LocalDateTime.now());
        PagedResponse<PublicUserResponseDto> paged = new PagedResponse<>(List.of(u1), 0, 20, 1L, 1, true);

        when(userService.getMembersDirectoryPublic(null, 0, 20)).thenReturn(paged);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].name", is("Alice")));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Should return public profile when found (no email)")
    void shouldGetUserByIdWhenFound() throws Exception {
        PublicUserResponseDto user = new PublicUserResponseDto(5L, "Charlie", "charlie_cf", 2000, LocalDateTime.now());

        when(userService.getPublicUserById(5L)).thenReturn(user);

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
        when(userService.getPublicUserById(999L)).thenThrow(new ResourceNotFoundException("User not found with id: 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("User not found with id: 999")));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should update Codeforces handle when caller owns the resource")
    void shouldUpdateCodeforcesHandle() throws Exception {
        authenticateAs("alice@example.com", "ROLE_USER");

        UserResponseDto caller = userDto(1L, "Alice", "alice@example.com", "alice_cf", 1600, Role.ROLE_USER);
        UserResponseDto updatedUser = userDto(1L, "Alice", "alice@example.com", "new_cf_handle", 1600, Role.ROLE_USER);

        when(userService.getUserByEmail("alice@example.com")).thenReturn(caller);
        when(userService.updateCodeforcesHandle(eq(1L), any(UpdateHandleRequest.class))).thenReturn(updatedUser);

        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"new_cf_handle\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.codeforcesHandle", is("new_cf_handle")));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should return 403 when caller tries to update another user's handle (IDOR guard)")
    void shouldReturn403WhenUpdatingOtherUsersHandle() throws Exception {
        // Bob (id=2) tries to update Alice's (id=1) handle — must be rejected.
        authenticateAs("bob@example.com", "ROLE_USER");

        UserResponseDto bob = userDto(2L, "Bob", "bob@example.com", "bob_cf", 1800, Role.ROLE_USER);
        when(userService.getUserByEmail("bob@example.com")).thenReturn(bob);

        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"stolen_handle\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/handle - Should return 400 Bad Request on blank handle")
    void shouldReturn400OnBlankHandle() throws Exception {
        mockMvc.perform(put("/api/users/1/handle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"handle\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    /**
     * Builds a {@link UserResponseDto} carrying only the fields these tests assert
     * on, leaving the Phase 2 profile fields null.
     *
     * <p>The record has eighteen components. Constructing it inline made every
     * test that touched it a wall of nulls, and adding a field meant editing five
     * call sites that did not care about it.</p>
     */
    private UserResponseDto userDto(Long id, String name, String email,
                                    String codeforcesHandle, Integer rating, Role role) {
        return new UserResponseDto(
                id, name, email,
                null,               // avatarUrl
                null,               // phoneNumber
                codeforcesHandle, rating,
                null, null,         // leetcodeHandle, leetcodeRating
                null, null,         // codechefUrl, atcoderUrl
                null, null,         // githubUrl, linkedinUrl
                null,               // clubRole
                null,               // batchYear
                role,
                LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
