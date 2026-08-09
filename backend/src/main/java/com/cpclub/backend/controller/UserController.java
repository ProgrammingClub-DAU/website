package com.cpclub.backend.controller;

import com.cpclub.backend.dto.UpdateHandleRequest;
import com.cpclub.backend.dto.UserProfileUpdateRequest;
import com.cpclub.backend.dto.UserResponseDto;
import com.cpclub.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserResponseDto>> getLeaderboard() {
        List<UserResponseDto> leaderboard = userService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @PutMapping("/{id}/handle")
    public ResponseEntity<UserResponseDto> updateCodeforcesHandle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHandleRequest request) {
        UserResponseDto updatedUser = userService.updateCodeforcesHandle(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{id}/profile")
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        UserResponseDto updatedUser = userService.updateUserProfile(id, request);
        return ResponseEntity.ok(updatedUser);
    }
}
