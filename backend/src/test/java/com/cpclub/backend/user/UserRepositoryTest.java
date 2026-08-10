package com.cpclub.backend.user;

import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
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

        Page<User> leaderboard = userRepository.findAllByOrderByRatingDescNullsLast(PageRequest.of(0, 10));

        assertEquals(2, leaderboard.getContent().size());
        assertEquals(2400, leaderboard.getContent().get(0).getRating());
        assertEquals(1200, leaderboard.getContent().get(1).getRating());
    }

    @Test
    @DisplayName("Should detect existing email")
    void shouldDetectExistingEmail() {
        userRepository.save(new User("Bob", "bob@example.com", "hashed", Role.ROLE_USER));

        assertTrue(userRepository.existsByEmail("bob@example.com"));
        assertFalse(userRepository.existsByEmail("missing@example.com"));
    }
}
