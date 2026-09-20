package com.cpclub.backend.user.service;

import com.cpclub.backend.user.entity.ClubRole;
import java.util.Comparator;
import java.util.Arrays;
import com.cpclub.backend.common.dto.PagedResponse;
import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.user.dto.PublicUserResponseDto;
import com.cpclub.backend.user.dto.UpdateClubRoleRequest;
import com.cpclub.backend.user.dto.UpdateHandleRequest;
import com.cpclub.backend.user.dto.UpdateRoleRequest;
import com.cpclub.backend.user.dto.UserLookupDto;
import com.cpclub.backend.user.dto.UserProfileUpdateRequest;
import com.cpclub.backend.user.dto.UserResponseDto;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import com.cpclub.backend.codeforces.service.CodeforcesSyncService;
import com.cpclub.backend.leetcode.service.LeetCodeSyncService;
import com.cpclub.backend.codeforces.repository.CfSolveRepository;
import com.cpclub.backend.stats.repository.ContestParticipationRepository;
import com.cpclub.backend.common.model.Platform;
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
    private final CodeforcesSyncService codeforcesSyncService;
    private final LeetCodeSyncService leetCodeSyncService;
    private final CfSolveRepository cfSolveRepository;
    private final ContestParticipationRepository contestParticipationRepository;

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
     * Checks whether a Codeforces handle is linked to any registered member.
     * Used by the Next.js CF proxy to gate arbitrary handle lookups.
     *
     * @param handle Codeforces handle to check (case-insensitive)
     * @return true if a registered member owns this handle
     */
    @Transactional(readOnly = true)
    public boolean codeforcesHandleExists(String handle) {
        return Boolean.TRUE.equals(userRepository.existsByCodeforcesHandleIgnoreCase(handle));
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
     * Retrieves all registered users in the database, oldest account first.
     *
     * <p>Ordered explicitly. {@code findAll()} with no sort leaves the order to
     * the database, and PostgreSQL returns rows in whatever order they happen to
     * sit in the table -- which changes whenever a row is updated. The admin
     * members table therefore reshuffled itself every time anyone edited a
     * profile or a club role, and a row an admin was looking at moved.</p>
     *
     * @return list of user details DTOs, by id ascending
     */
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
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
     * Returns full DTO including email â€” for admin/authenticated use only.
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
     * @param viewerIsAdmin whether the caller holds ROLE_ADMIN
     * @return standardized paginated wrapper containing public-safe user projections
     */
    @Transactional(readOnly = true)
    public PagedResponse<PublicUserResponseDto> getMembersDirectoryPublic(
            String query, int page, int size, boolean viewerIsAdmin) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        Page<User> userPage = userRepository.searchUsers(query, pageable);

        List<PublicUserResponseDto> content = userPage.getContent().stream()
                .map(user -> PublicUserResponseDto.fromEntity(user, viewerIsAdmin))
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
     * Public-safe profile lookup by ID â€” omits email, role, and updatedAt.
     * Use this for the public-facing {@code GET /api/users/{id}} endpoint.
     *
     * @param id user ID
     * @param viewerIsAdmin whether the caller holds ROLE_ADMIN
     * @return public-safe immutable user projection
     * @throws ResourceNotFoundException if user ID does not exist
     */
    @Transactional(readOnly = true)
    public PublicUserResponseDto getPublicUserById(Long id, boolean viewerIsAdmin) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return PublicUserResponseDto.fromEntity(user, viewerIsAdmin);
    }

    /**
     * The club's serving office bearers, in hierarchy order.
     *
     * <p>Separate from the paginated directory rather than a filter on it, for
     * two reasons. The members page needs the whole team at once -- a Convenor
     * who fell onto page two because of their surname would simply be missing --
     * and the ordering the page wants is the club's hierarchy, which is not
     * something a JPQL {@code ORDER BY} on an enum column can express: that sorts
     * the stored strings, putting ASSOCIATE_CORE above CONVENOR.</p>
     *
     * <p>Sorting in memory is safe here precisely because the list is bounded.
     * This is the committee, a few dozen people at most, not the membership.</p>
     *
     * @param viewerIsAdmin whether the caller holds ROLE_ADMIN
     * @return office bearers, Convenor first, then by name within each post
     */
    @Transactional(readOnly = true)
    public List<PublicUserResponseDto> getTeam(boolean viewerIsAdmin) {
        List<ClubRole> posts = Arrays.stream(ClubRole.values())
                .filter(ClubRole::isOfficeBearer)
                .toList();

        return userRepository.findByClubRoleIn(posts).stream()
                .sorted(Comparator
                        .comparingInt((User user) -> user.getClubRole().hierarchyRank())
                        .thenComparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> PublicUserResponseDto.fromEntity(user, viewerIsAdmin))
                .toList();
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

        boolean handleChanged = !handle.equalsIgnoreCase(user.getCodeforcesHandle());
        user.setCodeforcesHandle(handle);
        if (handleChanged) {
            user.setCfLastSubmissionId(null);
            cfSolveRepository.deleteByUser(user);
            contestParticipationRepository.deleteByUserAndPlatform(user, Platform.CODEFORCES);
        }
        User savedUser = userRepository.save(user);
        codeforcesSyncService.syncSingleUser(savedUser);
        log.info("Updated Codeforces handle for user id {} to '{}'", userId, handle);
        return UserResponseDto.fromEntity(savedUser);
    }

    /**
     * Updates full user profile details.
     *
     * <p>Both competitive-programming handles are unique columns, so each is
     * checked against the rest of the membership before being written. The
     * comparison excludes the member's own current handle, so re-saving an
     * unchanged profile is not rejected as a duplicate of itself.</p>
     *
     * <p>An external sync runs only when the handle it belongs to actually
     * changed. Syncing unconditionally meant every profile save â€” a name edit, a
     * new LinkedIn URL â€” spent a permit on the shared rate limiter that the
     * scheduled jobs also queue behind.</p>
     *
     * <p>The link-only fields are stored as given. CodeChef and AtCoder publish no
     * stable API to verify them against, so validation stops at the length limit
     * the column enforces.</p>
     *
     * @param userId user ID to modify
     * @param request new profile values
     * @return updated user details DTO
     * @throws BadRequestException if either handle belongs to another member
     */
    @Transactional
    public UserResponseDto updateProfile(Long userId, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setName(request.name());
        user.setPhoneNumber(trimToNull(request.phoneNumber()));
        user.setCodechefUrl(trimToNull(request.codechefUrl()));
        user.setAtcoderUrl(trimToNull(request.atcoderUrl()));
        user.setGithubUrl(trimToNull(request.githubUrl()));
        user.setLinkedinUrl(trimToNull(request.linkedinUrl()));
        user.setAvatarUrl(trimToNull(request.avatarUrl()));
        user.setAcademicYear(request.academicYear());

        boolean codeforcesChanged = false;
        if (request.codeforcesHandle() != null && !request.codeforcesHandle().isBlank()) {
            String handle = request.codeforcesHandle().trim();
            if (userRepository.existsByCodeforcesHandle(handle) &&
                !handle.equalsIgnoreCase(user.getCodeforcesHandle())) {
                throw new BadRequestException("Codeforces handle '" + handle + "' is already in use!");
            }
            codeforcesChanged = !handle.equalsIgnoreCase(user.getCodeforcesHandle());
            user.setCodeforcesHandle(handle);
            if (codeforcesChanged) {
                user.setCfLastSubmissionId(null);
                cfSolveRepository.deleteByUser(user);
                contestParticipationRepository.deleteByUserAndPlatform(user, Platform.CODEFORCES);
            }
        }

        boolean leetcodeChanged = false;
        if (request.leetcodeHandle() != null && !request.leetcodeHandle().isBlank()) {
            String handle = request.leetcodeHandle().trim();
            if (Boolean.TRUE.equals(userRepository.existsByLeetcodeHandle(handle)) &&
                !handle.equalsIgnoreCase(user.getLeetcodeHandle())) {
                throw new BadRequestException("LeetCode handle '" + handle + "' is already in use!");
            }
            leetcodeChanged = !handle.equalsIgnoreCase(user.getLeetcodeHandle());
            user.setLeetcodeHandle(handle);
        }

        User saved = userRepository.save(user);

        if (codeforcesChanged) {
            codeforcesSyncService.syncSingleUser(saved);
        }
        if (leetcodeChanged) {
            leetCodeSyncService.syncSingleUser(saved);
        }
        return UserResponseDto.fromEntity(saved);
    }

    /**
     * Normalizes an optional free-text field.
     *
     * <p>An empty string and a null both mean "not supplied" to the client, but
     * only one of them means it in the database: storing the empty string in a
     * nullable column makes "cleared" indistinguishable from "set to nothing" for
     * every later reader, and in a unique column two blanks collide.</p>
     *
     * @param value raw client value
     * @return trimmed value, or null when absent or blank
     */
    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Assigns a member's position in the club.
     *
     * <p>Changes the title only. API authorization lives on {@code Role} and is
     * changed by {@link #updateUserRole}: making someone Convenor here does not
     * make them an admin.</p>
     *
     * @param userId user ID to modify
     * @param request the position to assign
     * @return updated user details DTO
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional
    public UserResponseDto updateClubRole(Long userId, UpdateClubRoleRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setClubRole(request.clubRole());
        User saved = userRepository.save(user);
        log.info("Updated club role for user id {} to {}", userId, request.clubRole());
        return UserResponseDto.fromEntity(saved);
    }

    /**
     * Finds members for the attendance panel.
     *
     * <p>The panel used to take the database id, which no member knows and no
     * admin can see without opening the members table first. This takes what is
     * on a student card instead: the student ID in front of a DAU address, so
     * 202401226 resolves 202401226@dau.ac.in. A full email, a name or a
     * Codeforces handle work too, because members who joined with another
     * address have no student ID to type.</p>
     *
     * @param query student ID, email address, name fragment or Codeforces handle
     * @return up to ten matches, ordered by name; empty when nothing matches
     * @throws BadRequestException if the query is too short to narrow anything down
     */
    @Transactional(readOnly = true)
    public List<UserLookupDto> searchForAttendance(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            throw new BadRequestException(
                    "Enter at least 2 characters: a student ID, an email address or a name.");
        }

        return userRepository.searchForAttendance(trimmed, PageRequest.of(0, 10))
                .stream()
                .map(UserLookupDto::fromEntity)
                .toList();
    }

    /**
     * Full member record for the admin event-attendance panel.
     *
     * <p>Returns {@link UserLookupDto}, which carries the member's phone number,
     * so every caller must be admin-gated.</p>
     *
     * @param id user ID
     * @return full admin-facing member record
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public UserLookupDto lookupUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserLookupDto.fromEntity(user);
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

        requireAnAdminRemains(user, request.role());

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        requireAnAdminRemains(user, Role.ROLE_USER);

        userRepository.deleteById(userId);
        log.info("Deleted user id {}", userId);
    }

    /**
     * Refuses a change that would leave the club with no administrator.
     *
     * <p>An administrator here is an ordinary member whom another administrator
     * promoted, so the admin list can only be refilled from inside the admin
     * screens. Emptying it locks everyone out of those screens, and the only way
     * back is an UPDATE run by hand against the production database.</p>
     *
     * <p>A deletion passes {@link Role#ROLE_USER} as the new role: the account is
     * about to stop being an administrator by disappearing.</p>
     *
     * @param user member being demoted or deleted
     * @param newRole role that member is about to hold
     * @throws BadRequestException if the change would remove the last administrator
     */
    private void requireAnAdminRemains(User user, Role newRole) {
        boolean losesAdmin = user.getRole() == Role.ROLE_ADMIN && newRole != Role.ROLE_ADMIN;
        if (losesAdmin && userRepository.countByRole(Role.ROLE_ADMIN) <= 1) {
            throw new BadRequestException(
                    "At least one administrator must remain. Promote another member first.");
        }
    }
}
