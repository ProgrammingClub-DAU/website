package com.cpclub.backend.service;

import com.cpclub.backend.dto.UpdateHandleRequest;
import com.cpclub.backend.dto.UserProfileUpdateRequest;
import com.cpclub.backend.dto.UserResponseDto;
import com.cpclub.backend.entity.User;
import com.cpclub.backend.exception.ResourceNotFoundException;
import com.cpclub.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CodeforcesSyncService codeforcesSyncService;

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponseDto.fromEntity(user);
    }

    public List<UserResponseDto> getLeaderboard() {
        return userRepository.findAllByOrderByRatingDesc()
                .stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDto updateCodeforcesHandle(Long id, UpdateHandleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setCodeforcesHandle(request.getCodeforcesHandle().trim());
        User updatedUser = userRepository.save(user);
        
        // Immediately try to sync the rating
        codeforcesSyncService.syncUserRating(updatedUser);
        
        return UserResponseDto.fromEntity(updatedUser);
    }

    @Transactional
    public UserResponseDto updateUserProfile(Long id, UserProfileUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            user.setName(request.getName().trim());
        }
        if (request.getCodeforcesHandle() != null) {
            user.setCodeforcesHandle(request.getCodeforcesHandle().trim());
        }

        User updatedUser = userRepository.save(user);

        // Instantly sync the rating if the handle was updated
        if (request.getCodeforcesHandle() != null) {
            codeforcesSyncService.syncUserRating(updatedUser);
        }

        return UserResponseDto.fromEntity(updatedUser);
    }
}
