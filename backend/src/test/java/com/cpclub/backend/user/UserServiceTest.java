package com.cpclub.backend.user;

import com.cpclub.backend.user.UpdateHandleRequest;
import com.cpclub.backend.user.UserProfileUpdateRequest;
import com.cpclub.backend.user.UserResponseDto;
import com.cpclub.backend.entity.Role;
import com.cpclub.backend.entity.User;
import com.cpclub.backend.codeforces.CodeforcesSyncService;
import com.cpclub.backend.common.ResourceNotFoundException;
import com.cpclub.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private CodeforcesSyncService codeforcesSyncService;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        codeforcesSyncService = mock(CodeforcesSyncService.class);
        userService = new UserService(userRepository, codeforcesSyncService);
    }

    @Test
    @DisplayName("Should return all users converted to DTOs")
    void shouldGetAllUsers() {
        User u1 = new User("User One", "one@example.com", "pass1", Role.ROLE_USER);
        u1.setId(1L);
        User u2 = new User("User Two", "two@example.com", "pass2", Role.ROLE_ADMIN);
        u2.setId(2L);

        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<UserResponseDto> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("User One", result.get(0).name());
        assertEquals("User Two", result.get(1).name());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Should return user by ID when user exists")
    void shouldGetUserByIdWhenFound() {
        User user = new User("John Doe", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(10L);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserResponseDto dto = userService.getUserById(10L);

        assertNotNull(dto);
        assertEquals(10L, dto.id());
        assertEquals("John Doe", dto.name());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user ID does not exist")
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("Should update Codeforces handle")
    void shouldUpdateCodeforcesHandle() {
        User user = new User("John", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateHandleRequest request = new UpdateHandleRequest("tourist_pro");
        UserResponseDto result = userService.updateCodeforcesHandle(1L, request);

        assertNotNull(result);
        assertEquals("tourist_pro", result.codeforcesHandle());
        verify(userRepository, times(1)).save(user);
        verify(codeforcesSyncService, times(1)).syncUserRating(user);
    }

    @Test
    @DisplayName("Should update User profile name and handle")
    void shouldUpdateUserProfile() {
        User user = new User("Old Name", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileUpdateRequest request = new UserProfileUpdateRequest("New Name", "new_handle");
        UserResponseDto result = userService.updateUserProfile(1L, request);

        assertEquals("New Name", result.name());
        assertEquals("new_handle", result.codeforcesHandle());
        verify(codeforcesSyncService, times(1)).syncUserRating(user);
    }

    @Test
    @DisplayName("Should fetch leaderboard ordered by rating descending")
    void shouldGetLeaderboard() {
        User u1 = new User("High Rated", "h@example.com", "p", Role.ROLE_USER);
        u1.setRating(2400);
        User u2 = new User("Low Rated", "l@example.com", "p", Role.ROLE_USER);
        u2.setRating(1200);

        when(userRepository.findAllByOrderByRatingDesc()).thenReturn(Arrays.asList(u1, u2));

        List<UserResponseDto> leaderboard = userService.getLeaderboard();

        assertEquals(2, leaderboard.size());
        assertEquals(2400, leaderboard.get(0).rating());
        assertEquals(1200, leaderboard.get(1).rating());
    }
}
