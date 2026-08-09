package com.cpclub.backend.controller;

import com.cpclub.backend.dto.UpdateHandleRequest;
import com.cpclub.backend.dto.UserProfileUpdateRequest;
import com.cpclub.backend.dto.UserResponseDto;
import com.cpclub.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Users", description = "User directory, profile, and leaderboard APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users", description = "Returns all registered club members as public user DTOs.")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a single user profile by database ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = com.cpclub.backend.dto.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get leaderboard", description = "Returns users ordered by Codeforces rating descending.")
    @ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    public ResponseEntity<List<UserResponseDto>> getLeaderboard() {
        return ResponseEntity.ok(userService.getLeaderboard());
    }

    @PutMapping("/{id}/handle")
    @Operation(summary = "Update Codeforces handle", description = "Updates the authenticated user's Codeforces handle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Handle updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid handle format",
                    content = @Content(schema = @Schema(implementation = com.cpclub.backend.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = com.cpclub.backend.dto.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponseDto> updateCodeforcesHandle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHandleRequest request) {
        return ResponseEntity.ok(userService.updateCodeforcesHandle(id, request));
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update user profile", description = "Updates optional profile fields such as name and Codeforces handle.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = com.cpclub.backend.dto.ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = com.cpclub.backend.dto.ErrorResponse.class)))
    })
    public ResponseEntity<UserResponseDto> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserProfile(id, request));
    }
}
