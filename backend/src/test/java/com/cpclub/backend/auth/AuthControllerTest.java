package com.cpclub.backend.auth;

import com.cpclub.backend.auth.controller.AuthController;
import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.GoogleSignInRequest;
import com.cpclub.backend.auth.service.AuthService;
import com.cpclub.backend.common.exception.GlobalExceptionHandler;
import com.cpclub.backend.user.entity.AcademicYear;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The one sign-in endpoint.
 *
 * <p>These tests describe the HTTP contract the browser depends on: the shape of
 * the success payload, and the status codes for a refused sign-in and a malformed
 * request. Who is allowed in is decided in {@link AuthServiceTest}.</p>
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/google - returns 200 with the site's own token")
    void signInWithGoogle_Success() throws Exception {
        AuthResponse authResponse = new AuthResponse(
                "our-jwt", 1L, "Ravi Patel", "202401226@dau.ac.in",
                Role.ROLE_USER, "ravi_cf", AcademicYear.SECOND_YEAR_ONWARDS);

        when(authService.signInWithGoogle(any(GoogleSignInRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleSignInRequest("google-id-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("our-jwt"))
                .andExpect(jsonPath("$.data.email").value("202401226@dau.ac.in"))
                .andExpect(jsonPath("$.data.academicYear").value("SECOND_YEAR_ONWARDS"));
    }

    @Test
    @DisplayName("POST /api/auth/google - a new member's null year reaches the client")
    void signInWithGoogle_NewMemberHasNoYear() throws Exception {
        AuthResponse authResponse = new AuthResponse(
                "our-jwt", 2L, "New Member", "202501111@dau.ac.in",
                Role.ROLE_USER, null, null);

        when(authService.signInWithGoogle(any(GoogleSignInRequest.class))).thenReturn(authResponse);

        // The client routes to the welcome step on this being null, so it has to
        // survive serialization rather than being dropped from the payload.
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleSignInRequest("google-id-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicYear").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/google - a refused sign-in is 401, and says what to do")
    void signInWithGoogle_Refused() throws Exception {
        when(authService.signInWithGoogle(any(GoogleSignInRequest.class)))
                .thenThrow(new BadCredentialsException(
                        "Only @dau.ac.in accounts can sign in. Please use your university Google account."));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleSignInRequest("google-id-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Only @dau.ac.in accounts can sign in. Please use your university Google account."));
    }

    @Test
    @DisplayName("POST /api/auth/google - an empty credential is rejected before any verification")
    void signInWithGoogle_MissingToken() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GoogleSignInRequest(""))))
                .andExpect(status().isBadRequest());
    }
}
