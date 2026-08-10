package com.cpclub.backend.auth.service;

import com.cpclub.backend.auth.dto.AuthResponse;
import com.cpclub.backend.auth.dto.LoginRequest;
import com.cpclub.backend.auth.dto.RegisterRequest;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.security.jwt.JwtUtils;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered!");
        }

        if (request.codeforcesHandle() != null && !request.codeforcesHandle().isBlank()) {
            if (userRepository.existsByCodeforcesHandle(request.codeforcesHandle().trim())) {
                throw new BadRequestException("Codeforces handle is already registered!");
            }
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .codeforcesHandle(request.codeforcesHandle() != null && !request.codeforcesHandle().isBlank() ? request.codeforcesHandle().trim() : null)
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with email: {}", email);

        String jwt = jwtUtils.generateTokenFromEmail(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());

        return new AuthResponse(
                jwt,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getCodeforcesHandle()
        );
    }

    public AuthResponse authenticateUser(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found after authentication"));

        log.info("Authenticated user: {}", email);

        return new AuthResponse(
                jwt,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCodeforcesHandle()
        );
    }
}
