package com.cpclub.backend.user.controller;

import com.cpclub.backend.common.dto.ApiResponse;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UpdateRoleRequest;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for fetching and managing user profiles and member directory")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get members directory (paginated & searchable)")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponseDto>>> getMembersDirectory(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PagedResponse<UserResponseDto> response = userService.getMembersDirectory(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(response, "Fetched members directory successfully"));
    }

    @GetMapping("/all")
    @Operation(summary = "Get all users (unpaginated)")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Fetched all users successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "Fetched user successfully"));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current authenticated user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserResponseDto user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "Fetched current user profile successfully"));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current authenticated user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        UserResponseDto currentUser = userService.getUserByEmail(userDetails.getUsername());
        UserResponseDto updatedUser = userService.updateProfile(currentUser.id(), request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Profile updated successfully"));
    }

    @PutMapping("/{id}/handle")
    @Operation(summary = "Update Codeforces handle")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateCodeforcesHandle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHandleRequest request
    ) {
        UserResponseDto updatedUser = userService.updateCodeforcesHandle(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "Codeforces handle updated successfully"));
    }

    @PutMapping("/{id}/role")
    @Operation(summary = "Update user role (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UserResponseDto updatedUser = userService.updateUserRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(updatedUser, "User role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}
