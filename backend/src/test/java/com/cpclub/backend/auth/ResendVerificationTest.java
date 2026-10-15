package com.cpclub.backend.auth;

import com.cpclub.backend.auth.controller.AuthController;
import com.cpclub.backend.auth.service.AuthService;
import com.cpclub.backend.auth.service.EmailVerificationService;
import com.cpclub.backend.auth.service.ResendRateLimiter;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.cpclub.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResendVerificationTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserService userService;

    @Mock
    private EmailVerificationService emailVerificationService;

    @Mock
    private UserRepository userRepository;

    private ResendRateLimiter resendRateLimiter;

    private User testUser;

    @BeforeEach
    void setUp() {
        resendRateLimiter = new ResendRateLimiter();
        AuthController authController = new AuthController(
                authService, userService, emailVerificationService, resendRateLimiter, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        testUser = User.builder()
                .id(42L)
                .name("Ravi")
                .email("ravi@dau.ac.in")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("Two resends within 60 seconds - second returns 429 with Retry-After header")
    void twoResendsWithin60Seconds_secondReturns429WithRetryAfter() throws Exception {
        when(userRepository.findByEmail("ravi@dau.ac.in")).thenReturn(Optional.of(testUser));

        // First attempt should succeed (200 OK)
        mockMvc.perform(post("/api/auth/resend-verification")
                        .principal(() -> "ravi@dau.ac.in"))
                .andExpect(status().isOk());

        verify(emailVerificationService, times(1)).issue(testUser);

        // Immediate second attempt should be rate limited (429 Too Many Requests with Retry-After)
        mockMvc.perform(post("/api/auth/resend-verification")
                        .principal(() -> "ravi@dau.ac.in"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));

        // issue() was NOT called a second time
        verify(emailVerificationService, times(1)).issue(testUser);
    }
}
