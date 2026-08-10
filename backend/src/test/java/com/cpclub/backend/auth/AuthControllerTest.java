package com.cpclub.backend.auth;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.LoginRequest;
import com.cpclub.backend.auth.dto.RegisterRequest;
import com.cpclub.backend.common.BadRequestException;
import com.cpclub.backend.common.GlobalExceptionHandler;
import com.cpclub.backend.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 201 Created and AuthResponse")
    void registerUser_Success() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "Password123!",
                "tourist"
        );

        AuthResponse authResponse = new AuthResponse(
                "jwt_token_sample", 1L, "John Doe", "john@example.com", Role.ROLE_USER, "tourist"
        );

        when(authService.registerUser(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt_token_sample"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 Bad Request on duplicate email")
    void registerUser_DuplicateEmail() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "John Doe",
                "john@example.com",
                "Password123!",
                null
        );

        when(authService.registerUser(any(RegisterRequest.class)))
                .thenThrow(new BadRequestException("Email is already registered!"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered!"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 200 OK and AuthResponse")
    void loginUser_Success() throws Exception {
        LoginRequest loginRequest = new LoginRequest("john@example.com", "Password123!");

        AuthResponse authResponse = new AuthResponse(
                "jwt_token_sample", 1L, "John Doe", "john@example.com", Role.ROLE_USER, "tourist"
        );

        when(authService.authenticateUser(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt_token_sample"));
    }
}
