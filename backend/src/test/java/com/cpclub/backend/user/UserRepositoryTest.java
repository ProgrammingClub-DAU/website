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
    @DisplayName("A student ID finds the DAU address that starts with it")
    void shouldFindByStudentIdPrefix() {
        // What an admin can read off a student card. The database id, which the
        // panel used to ask for, is not something anybody knows.
        userRepository.save(new User("Ravi", "202401226@dau.ac.in", "hashed", Role.ROLE_USER));
        userRepository.save(new User("Other", "202401999@dau.ac.in", "hashed", Role.ROLE_USER));

        var found = userRepository.searchForAttendance("202401226", PageRequest.of(0, 10));

        assertEquals(1, found.size());
        assertEquals("Ravi", found.get(0).getName());
    }

    @Test
    @DisplayName("A full email address matches exactly")
    void shouldFindByFullEmail() {
        userRepository.save(new User("Ravi", "202401226@dau.ac.in", "hashed", Role.ROLE_USER));

        var found = userRepository.searchForAttendance("202401226@DAU.AC.IN", PageRequest.of(0, 10));

        assertEquals(1, found.size());
    }

    @Test
    @DisplayName("Part of a name matches, for members with no student ID in their address")
    void shouldFindByNameFragment() {
        userRepository.save(new User("Meher Shah", "meher@gmail.com", "hashed", Role.ROLE_USER));

        var found = userRepository.searchForAttendance("meher", PageRequest.of(0, 10));

        assertEquals(1, found.size());
        assertEquals("Meher Shah", found.get(0).getName());
    }

    @Test
    @DisplayName("A student ID that belongs to nobody returns nothing")
    void shouldReturnNothingForAnUnknownStudentId() {
        userRepository.save(new User("Ravi", "202401226@dau.ac.in", "hashed", Role.ROLE_USER));

        assertTrue(userRepository.searchForAttendance("999999999", PageRequest.of(0, 10)).isEmpty());
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
