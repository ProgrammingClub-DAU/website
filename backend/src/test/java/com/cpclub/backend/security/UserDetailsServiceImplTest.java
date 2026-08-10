package com.cpclub.backend.security;

import com.cpclub.backend.security.service.UserDetailsServiceImpl;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    private UserRepository userRepository;
    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userDetailsService = new UserDetailsServiceImpl(userRepository);
    }

    @Test
    void loadUserByUsername_returnsSecurityPrincipalForExistingEmail() {
        User user = new User("Alice", "alice@example.com", "bcrypt-hash", Role.ROLE_ADMIN);
        user.setId(3L);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("alice@example.com");

        assertEquals("alice@example.com", result.getUsername());
        assertEquals("bcrypt-hash", result.getPassword());
        assertEquals("ROLE_ADMIN", result.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void loadUserByUsername_throwsWhenEmailIsMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@example.com"));
    }
}
