package com.cpclub.backend.user;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UpdateRoleRequest;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.cpclub.backend.user.service.UserService;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
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
    private CodeforcesSyncService codeforcesSyncService;
    private LeetCodeSyncService leetCodeSyncService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        codeforcesSyncService = mock(CodeforcesSyncService.class);
        leetCodeSyncService = mock(LeetCodeSyncService.class);
        userService = new UserService(userRepository, codeforcesSyncService, leetCodeSyncService);
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
    @DisplayName("Should reject a Codeforces handle already linked to another user")
    void shouldRejectDuplicateCodeforcesHandle() {
        User user = new User("John", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByCodeforcesHandle("taken_handle")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> userService.updateCodeforcesHandle(1L, new UpdateHandleRequest("taken_handle")));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should update User profile name and handle")
    void shouldUpdateUserProfile() {
        User user = new User("Old Name", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByCodeforcesHandle("new_handle")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserProfileUpdateRequest request = profileUpdate("New Name", "new_handle", null);
        UserResponseDto result = userService.updateProfile(1L, request);

        assertEquals("New Name", result.name());
        assertEquals("new_handle", result.codeforcesHandle());
    }

    @Test
    @DisplayName("Should update a user's role")
    void shouldUpdateUserRole() {
        User user = new User("John", "john@example.com", "pass", Role.ROLE_USER);
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponseDto result = userService.updateUserRole(1L, new UpdateRoleRequest(Role.ROLE_ADMIN));

        assertEquals(Role.ROLE_ADMIN, result.role());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should reject deletion of a user that does not exist")
    void shouldRejectDeletingMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(99L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("The last administrator cannot be demoted")
    void shouldRejectDemotingTheLastAdmin() {
        // Administrators are ordinary members promoted by another administrator,
        // so an empty admin list can only be repaired with SQL against production.
        User lastAdmin = new User("Lead", "lead@dau.ac.in", "pass", Role.ROLE_ADMIN);
        lastAdmin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(1L);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> userService.updateUserRole(1L, new UpdateRoleRequest(Role.ROLE_USER)));

        assertTrue(error.getMessage().contains("At least one administrator"));
        assertEquals(Role.ROLE_ADMIN, lastAdmin.getRole());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("An administrator can be demoted while another one remains")
    void shouldAllowDemotingAnAdminWhenAnotherRemains() {
        User admin = new User("Core", "core@dau.ac.in", "pass", Role.ROLE_ADMIN);
        admin.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(2L);
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponseDto result = userService.updateUserRole(2L, new UpdateRoleRequest(Role.ROLE_USER));

        assertEquals(Role.ROLE_USER, result.role());
    }

    @Test
    @DisplayName("The last administrator cannot be deleted")
    void shouldRejectDeletingTheLastAdmin() {
        User lastAdmin = new User("Lead", "lead@dau.ac.in", "pass", Role.ROLE_ADMIN);
        lastAdmin.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(1L);

        assertThrows(BadRequestException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("An administrator can be deleted while another one remains")
    void shouldAllowDeletingAnAdminWhenAnotherRemains() {
        User admin = new User("Core", "core@dau.ac.in", "pass", Role.ROLE_ADMIN);
        admin.setId(2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(2L);

        userService.deleteUser(2L);

        verify(userRepository).deleteById(2L);
    }

    @Test
    @DisplayName("Deleting an ordinary member never counts administrators")
    void shouldDeleteAnOrdinaryMemberWithoutCountingAdmins() {
        User member = new User("Student", "student@dau.ac.in", "pass", Role.ROLE_USER);
        member.setId(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(member));

        userService.deleteUser(3L);

        verify(userRepository).deleteById(3L);
        verify(userRepository, never()).countByRole(any());
    }

    /**
     * Builds a {@link UserProfileUpdateRequest} with only the fields under test set.
     *
     * <p>The record has nine components, most of them optional profile links these
     * tests do not exercise.</p>
     */
    private UserProfileUpdateRequest profileUpdate(String name, String codeforcesHandle,
                                                   String leetcodeHandle) {
        return new UserProfileUpdateRequest(
                name,
                null,               // phoneNumber
                codeforcesHandle,
                leetcodeHandle,
                null, null,         // codechefUrl, atcoderUrl
                null, null,         // githubUrl, linkedinUrl
                null                // avatarUrl
        );
    }
}
