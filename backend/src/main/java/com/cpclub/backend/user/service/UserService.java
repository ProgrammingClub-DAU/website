package com.cpclub.backend.user.service;

import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return UserResponseDto.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::fromEntity)
                .toList();
    }

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

    @Transactional
    public UserResponseDto updateUserRole(Long userId, UpdateRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setRole(request.role());
        User saved = userRepository.save(user);
        log.info("Updated role for user id {} to {}", userId, request.role());
        return UserResponseDto.fromEntity(saved);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        userRepository.deleteById(userId);
        log.info("Deleted user id {}", userId);
    }
}
