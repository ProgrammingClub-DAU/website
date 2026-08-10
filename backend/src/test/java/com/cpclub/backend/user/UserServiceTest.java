package com.cpclub.backend.user;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.cpclub.backend.user.service.UserService;
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
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
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
        when(userRepository.existsByCodeforcesHandle("tourist_pro")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UpdateHandleRequest request = new UpdateHandleRequest("tourist_pro");
        UserResponseDto result = userService.updateCodeforcesHandle(1L, request);

        assertNotNull(result);
        assertEquals("tourist_pro", result.codeforcesHandle());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("Should update User profile name and handle")
    void shouldUpdateUserProfile() {
        User user = new User("Old Name", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByCodeforcesHandle("new_handle")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileUpdateRequest request = new UserProfileUpdateRequest("New Name", "new_handle");
        UserResponseDto result = userService.updateProfile(1L, request);

        assertEquals("New Name", result.name());
        assertEquals("new_handle", result.codeforcesHandle());
    }
}
