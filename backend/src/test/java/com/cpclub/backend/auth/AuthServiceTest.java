package com.cpclub.backend.auth;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.LoginRequest;
import com.cpclub.backend.auth.dto.RegisterRequest;
import com.cpclub.backend.common.BadRequestException;
import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import com.cpclub.backend.security.jwt.JwtUtils;
import com.cpclub.backend.security.services.UserDetailsImpl;
import com.cpclub.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    private User sampleUser;
    private Authentication sampleAuth;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .name("Alice Developer")
                .email("alice@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .codeforcesHandle("alice_cp")
                .build();

        UserDetailsImpl userDetails = UserDetailsImpl.build(sampleUser);
        sampleAuth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    @DisplayName("Should successfully register a new user and return AuthResponse")
    void registerUser_Success() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Alice Developer",
                "alice@example.com",
                "Password123!",
                "alice_cp"
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.findByCodeforcesHandle("alice_cp")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(sampleAuth);
        when(jwtUtils.generateJwtToken(any(Authentication.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.registerUser(registerRequest);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.token());
        assertEquals("alice@example.com", response.email());
        assertEquals(Role.ROLE_USER, response.role());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when email already exists")
    void registerUser_DuplicateEmail() {
        RegisterRequest registerRequest = new RegisterRequest(
                "Alice Developer",
                "alice@example.com",
                "Password123!",
                null
        );

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should authenticate user and return valid token")
    void authenticateUser_Success() {
        LoginRequest loginRequest = new LoginRequest("alice@example.com", "Password123!");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(sampleAuth);
        when(jwtUtils.generateJwtToken(sampleAuth)).thenReturn("mock_jwt_token");

        AuthResponse response = authService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.token());
        assertEquals("alice@example.com", response.email());
    }
}
