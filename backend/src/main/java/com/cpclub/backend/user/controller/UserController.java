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

/**
 * REST controller for managing student profiles and retrieving member lists.
 * Secure endpoints are protected using method security annotations such as {@link PreAuthorize}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for fetching and managing user profiles and member directory")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves a paginated and filtered page of club members for the directory.
     *
     * @param query optional search query matching user's name or Codeforces handle
     * @param page zero-indexed page number
     * @param size number of items per page
     * @return paginated response containing user profiles
     */
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

    /**
     * Retrieves list of all users without pagination. Primarily for internal lookup.
     *
     * @return list of all users
     */
    @GetMapping("/all")
    @Operation(summary = "Get all users (unpaginated)")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users, "Fetched all users successfully"));
    }

    /**
     * Fetches public user profile information by user ID.
     *
     * @param id user ID
     * @return user profile details
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user, "Fetched user successfully"));
    }

    /**
     * Fetches private profile details of the authenticated caller.
     *
     * @param userDetails injected authentication details
     * @return caller's profile details
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current authenticated user profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserResponseDto user = userService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user, "Fetched current user profile successfully"));
    }

    /**
     * Updates profile details of the authenticated caller.
     *
     * @param userDetails injected authentication details
     * @param request profile fields to update
     * @return updated user profile details
     */
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

    /**
     * Updates Codeforces handle for a given user.
     *
     * @param id user ID to update
     * @param request body containing the handle
     * @return updated user profile details
     */
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

    /**
     * Assigns a role to a user. Restricted to Administrator role.
     *
     * @param id user ID to update
     * @param request role payload
     * @return updated user profile details
     */
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

    /**
     * Permanently deletes a user from the platform. Restricted to Administrator role.
     *
     * @param id user ID to delete
     * @return empty response body on success
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete user account (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, "User deleted successfully"));
    }
}

