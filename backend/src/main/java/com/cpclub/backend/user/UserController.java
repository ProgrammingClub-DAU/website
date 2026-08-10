package com.cpclub.backend.user;

import com.cpclub.backend.user.UpdateHandleRequest;
import com.cpclub.backend.user.UserProfileUpdateRequest;
import com.cpclub.backend.user.UserResponseDto;
import com.cpclub.backend.common.ApiResponse;
import com.cpclub.backend.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
    private final com.cpclub.backend.codeforces.CodeforcesSyncService codeforcesSyncService;

    @GetMapping
    @Operation(summary = "List all users", description = "Returns all registered club members as public user DTOs.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved successfully")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers(), "Users retrieved successfully");
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Returns a single user profile by database ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id), "User retrieved successfully");
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get leaderboard", description = "Returns users ordered by Codeforces rating descending.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Leaderboard retrieved successfully")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> getLeaderboard() {
        return ApiResponse.success(userService.getLeaderboard(), "Leaderboard retrieved successfully");
    }

    @PutMapping("/{id}/handle")
    @Operation(summary = "Update Codeforces handle", description = "Updates the authenticated user's Codeforces handle.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Handle updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid handle format",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserResponseDto>> updateCodeforcesHandle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHandleRequest request) {
        userService.updateCodeforcesHandle(id, request);
        codeforcesSyncService.syncRatingById(id);
        return ApiResponse.success(userService.getUserById(id), "Codeforces handle updated successfully");
    }

    @PutMapping("/{id}/profile")
    @Operation(summary = "Update user profile", description = "Updates optional profile fields such as name and Codeforces handle.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserResponseDto>> updateUserProfile(
            @PathVariable Long id,
            @Valid @RequestBody UserProfileUpdateRequest request) {
        userService.updateUserProfile(id, request);
        if (request.codeforcesHandle() != null) {
            codeforcesSyncService.syncRatingById(id);
        }
        return ApiResponse.success(userService.getUserById(id), "Profile updated successfully");
    }
}
