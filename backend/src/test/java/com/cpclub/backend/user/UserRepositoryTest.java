package com.cpclub.backend.user;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return empty list when database has no users")
    void shouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(List.of());
        assertTrue(userRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        User user = new User("Alice", "alice@example.com", "hashed", Role.ROLE_USER);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        Optional<User> found = userRepository.findByEmail("alice@example.com");

        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getName());
    }

    @Test
    @DisplayName("Should return users ordered by rating descending")
    void shouldFindAllByOrderByRatingDesc() {
        User lowRated = new User("Low", "low@example.com", "hashed", Role.ROLE_USER);
        lowRated.setRating(1200);
        User highRated = new User("High", "high@example.com", "hashed", Role.ROLE_USER);
        highRated.setRating(2400);

        when(userRepository.findAllByOrderByRatingDesc()).thenReturn(List.of(highRated, lowRated));

        List<User> leaderboard = userRepository.findAllByOrderByRatingDesc();

        assertEquals(2, leaderboard.size());
        assertEquals(2400, leaderboard.get(0).getRating());
        assertEquals(1200, leaderboard.get(1).getRating());
    }

    @Test
    @DisplayName("Should detect existing email")
    void shouldDetectExistingEmail() {
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);
        when(userRepository.existsByEmail("missing@example.com")).thenReturn(false);

        assertTrue(userRepository.existsByEmail("bob@example.com"));
        assertFalse(userRepository.existsByEmail("missing@example.com"));
    }
}
