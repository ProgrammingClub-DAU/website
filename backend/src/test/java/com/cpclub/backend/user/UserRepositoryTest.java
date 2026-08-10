package com.cpclub.backend.user;

import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should return empty list when database has no users")
    void shouldReturnEmptyListWhenNoUsers() {
        assertTrue(userRepository.findAll().isEmpty());
    }

    @Test
    @DisplayName("Should find user by email")
    void shouldFindByEmail() {
        User user = new User("Alice", "alice@example.com", "hashed", Role.ROLE_USER);
        userRepository.save(user);

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

        userRepository.save(lowRated);
        userRepository.save(highRated);

        List<User> leaderboard = userRepository.findAllByOrderByRatingDesc();

        assertEquals(2, leaderboard.size());
        assertEquals(2400, leaderboard.get(0).getRating());
        assertEquals(1200, leaderboard.get(1).getRating());
    }

    @Test
    @DisplayName("Should detect existing email")
    void shouldDetectExistingEmail() {
        userRepository.save(new User("Bob", "bob@example.com", "hashed", Role.ROLE_USER));

        assertTrue(userRepository.existsByEmail("bob@example.com"));
        assertFalse(userRepository.existsByEmail("missing@example.com"));
    }
}
