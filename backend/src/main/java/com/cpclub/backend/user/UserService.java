package com.cpclub.backend.user;

import com.cpclub.backend.entity.User;

import com.cpclub.backend.common.ResourceNotFoundException;
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

        user.setCodeforcesHandle(request.codeforcesHandle().trim());
        User updatedUser = userRepository.save(user);
        
        return UserResponseDto.fromEntity(updatedUser);
    }

    @Transactional
    public UserResponseDto updateUserProfile(Long id, UserProfileUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.name() != null && !request.name().trim().isEmpty()) {
            user.setName(request.name().trim());
        }

        if (request.codeforcesHandle() != null) {
            user.setCodeforcesHandle(request.codeforcesHandle().trim());
        }

        User updatedUser = userRepository.save(user);

        return UserResponseDto.fromEntity(updatedUser);
    }
}
