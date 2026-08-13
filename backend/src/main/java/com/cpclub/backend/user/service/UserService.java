package com.cpclub.backend.user.service;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.dto.PublicUserResponseDto;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UpdateRoleRequest;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class managing member directory lookups, user profiles,
 * and administrative role/deletion operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Resolves a user profile by database primary key.
     *
     * @param id user ID
     * @return mapped immutable user profile details DTO
     * @throws ResourceNotFoundException if user ID does not exist
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.fromEntity(user);
    }

    /**
     * Resolves a user profile by unique email address.
     *
     * @param email user email
     * @return mapped immutable user profile details DTO
     * @throws ResourceNotFoundException if user email does not exist
     */
    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponseDto.fromEntity(user);
    }

    /**
     * Retrieves all registered users in the database.
     *
     * @return list of user details DTOs
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::fromEntity)
                .toList();
    }

    /**
     * Searches and paginates members directory sorted alphabetically by name.
     *
     * @param query search filter matching name or Codeforces handle
     * @param page zero-indexed page number
     * @param size page size limit
     * @return standardized paginated wrapper containing results page
     */
    /**
     * Searches and paginates members directory sorted alphabetically by name.
     * Returns full DTO including email — for admin/authenticated use only.
     *
     * @param query search filter matching name or Codeforces handle
     * @param page zero-indexed page number
     * @param size page size limit
     * @return standardized paginated wrapper containing results page
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponseDto> getMembersDirectory(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<User> userPage = userRepository.searchUsers(query, pageable);

        List<UserResponseDto> content = userPage.getContent().stream()
                .map(UserResponseDto::fromEntity)
                .toList();

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    /**
     * Public-safe version of the members directory that omits email, role, and updatedAt.
     * Use this for the public-facing {@code GET /api/users} endpoint.
     *
     * @param query search filter matching name or Codeforces handle
     * @param page zero-indexed page number
     * @param size page size limit
     * @return standardized paginated wrapper containing public-safe user projections
     */
    @Transactional(readOnly = true)
    public PagedResponse<PublicUserResponseDto> getMembersDirectoryPublic(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<User> userPage = userRepository.searchUsers(query, pageable);

        List<PublicUserResponseDto> content = userPage.getContent().stream()
                .map(PublicUserResponseDto::fromEntity)
                .toList();

        return new PagedResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.isLast()
        );
    }

    /**
     * Public-safe profile lookup by ID — omits email, role, and updatedAt.
     * Use this for the public-facing {@code GET /api/users/{id}} endpoint.
     *
     * @param id user ID
     * @return public-safe immutable user projection
     * @throws ResourceNotFoundException if user ID does not exist
     */
    @Transactional(readOnly = true)
    public PublicUserResponseDto getPublicUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return PublicUserResponseDto.fromEntity(user);
    }

    /**
     * Updates the Codeforces handle of a user.
     * Ensures handle is not registered to another user to maintain unique mapping.
     *
     * @param userId user ID to modify
     * @param request update handle details
     * @return updated user details DTO
     * @throws BadRequestException if the handle is already registered
     */
    @Transactional
    public UserResponseDto updateCodeforcesHandle(Long userId, UpdateHandleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String handle = request.handle().trim();
        if (userRepository.existsByCodeforcesHandle(handle) &&
            !handle.equalsIgnoreCase(user.getCodeforcesHandle())) {
            throw new BadRequestException("Codeforces handle '" + handle + "' is already in use by another user!");
        }

        user.setCodeforcesHandle(handle);
        User savedUser = userRepository.save(user);
        log.info("Updated Codeforces handle for user id {} to '{}'", userId, handle);
        return UserResponseDto.fromEntity(savedUser);
    }

    /**
     * Updates full user profile details.
     * Checks Codeforces handle uniqueness constraints if updated.
     *
     * @param userId user ID to modify
     * @param request update details containing name and handle
     * @return updated user details DTO
     */
    @Transactional
    public UserResponseDto updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setName(request.name());
        if (request.codeforcesHandle() != null && !request.codeforcesHandle().isBlank()) {
            String handle = request.codeforcesHandle().trim();
            if (userRepository.existsByCodeforcesHandle(handle) &&
                !handle.equalsIgnoreCase(user.getCodeforcesHandle())) {
                throw new BadRequestException("Codeforces handle '" + handle + "' is already in use!");
            }
            user.setCodeforcesHandle(handle);
        }

        User saved = userRepository.save(user);
        return UserResponseDto.fromEntity(saved);
    }

    /**
     * Updates administrative authorization role of a user.
     *
     * @param userId user ID to modify
     * @param request role update payload
     * @return updated user details DTO
     */
    @Transactional
    public UserResponseDto updateUserRole(Long userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setRole(request.role());
        User saved = userRepository.save(user);
        log.info("Updated role for user id {} to {}", userId, request.role());
        return UserResponseDto.fromEntity(saved);
    }

    /**
     * Permanently deletes a user by ID.
     *
     * @param userId user ID to delete
     */
    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("Deleted user id {}", userId);
    }
}
