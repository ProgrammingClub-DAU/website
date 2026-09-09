package com.cpclub.backend.user.repository;

import com.cpclub.backend.leaderboard.dto.LeaderboardEntryProjection;
import com.cpclub.backend.user.entity.ClubRole;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository interface for managing {@link User} database entities.
 * Includes custom queries for paginated rating lookups (leaderboard) and matching handles/names.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds the member whose normalized email is used as the login principal.
     *
     * @param email unique member email
     * @return matching member when one exists
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks email uniqueness without loading the full entity during registration.
     *
     * @param email candidate email
     * @return whether a member already owns the email
     */
    Boolean existsByEmail(String email);

    /**
     * Checks whether a Codeforces handle is linked to any member.
     *
     * @param codeforcesHandle candidate external-account handle
     * @return whether the handle is already linked
     */
    Boolean existsByCodeforcesHandle(String codeforcesHandle);

    /**
     * Case-insensitive check used by the CF proxy endpoint to verify a handle
     * belongs to a registered member before forwarding it to Codeforces.
     * Codeforces treats handles case-insensitively, so this check must too.
     *
     * @param codeforcesHandle candidate external-account handle
     * @return whether the handle is linked to any registered member
     */
    Boolean existsByCodeforcesHandleIgnoreCase(String codeforcesHandle);

    /**
     * Locates a member by their linked Codeforces handle for synchronization workflows.
     *
     * @param codeforcesHandle external-account handle
     * @return matching member when one exists
     */
    Optional<User> findByCodeforcesHandle(String codeforcesHandle);

    /**
     * Returns members eligible for Codeforces synchronization.
     *
     * @return members that have supplied a Codeforces handle
     */
    List<User> findByCodeforcesHandleIsNotNull();

    /**
     * Returns members eligible for LeetCode synchronization.
     *
     * <p>The bulk sync iterates this rather than {@code findAll()}: most members
     * never link a LeetCode account, and each one that has costs a rate-limited
     * HTTP round trip.</p>
     *
     * @return members that have supplied a LeetCode handle
     */
    List<User> findByLeetcodeHandleIsNotNull();

    /**
     * Checks whether a LeetCode handle is linked to any member.
     *
     * <p>The column is unique, so without this check a duplicate handle would
     * surface as a constraint violation at flush time rather than a 400.</p>
     *
     * @param leetcodeHandle candidate external-account handle
     * @return whether the handle is already linked
     */
    Boolean existsByLeetcodeHandle(String leetcodeHandle);

    /**
     * Returns members holding any of the given club positions.
     *
     * <p>Backs the leaderboard's role filter. {@code STUDENTS} cannot use this
     * method, because its definition includes members with no role at all and
     * {@code IN} never matches NULL — see {@code LeaderboardService}.</p>
     *
     * @param roles positions to match
     * @return members holding one of them
     */
    List<User> findByClubRoleIn(List<ClubRole> roles);

    /**
     * Resolves paginated list of users ordered by rating descending.
     * Non-rated members (null ratings) are pushed to the end of the ranking list.
     *
     * @param pageable requested page and size
     * @return page of members in ranking order
     */
    @Query("SELECT u FROM User u ORDER BY u.rating DESC NULLS LAST, u.id ASC")
    Page<User> findAllByOrderByRatingDescNullsLast(Pageable pageable);

    /**
     * One page of the leaderboard for a given platform and membership slice, with
     * each member's rank already computed by the database.
     *
     * <p>Both variable parts are bound parameters compared against literals inside
     * the SQL rather than interpolated into it. {@code platform} and {@code filter}
     * arrive as enum names, so the set of possible values is closed, but building
     * the statement by concatenation would still make this the one place in the
     * codebase where a repository takes a caller-supplied fragment.</p>
     *
     * <p>The {@code WHERE} clause runs before the window function, so
     * {@code RANK()} numbers the filtered board: viewing the core team shows ranks
     * 1..n within the core team, not their positions in the club overall. That is
     * the intended reading of a filtered leaderboard.</p>
     *
     * <p>{@code STUDENTS} cannot be expressed as an {@code IN} list. Its definition
     * includes members with no position recorded, and {@code IN} never matches
     * NULL — every pre-Phase-2 account would vanish from the board.</p>
     *
     * <p>Column aliases are deliberately single lowercase words; see
     * {@link com.cpclub.backend.leaderboard.dto.LeaderboardEntryProjection}.</p>
     *
     * @param platform {@code CODEFORCES} or {@code LEETCODE}
     * @param filter {@code ALL}, {@code CORE}, {@code BATCH_REP} or {@code STUDENTS}
     * @param pageable requested page and size
     * @return one page of ranked members
     */
    @Query(value = """
            SELECT u.id AS id,
                   u.name AS name,
                   CASE WHEN CAST(:platform AS VARCHAR) = 'LEETCODE'
                        THEN u.leetcode_handle ELSE u.codeforces_handle END AS handle,
                   CASE WHEN CAST(:platform AS VARCHAR) = 'LEETCODE'
                        THEN u.leetcode_rating ELSE u.rating END AS rating,
                   u.club_role AS clubrole,
                   RANK() OVER (
                       ORDER BY CASE WHEN CAST(:platform AS VARCHAR) = 'LEETCODE'
                                     THEN u.leetcode_rating ELSE u.rating END DESC NULLS LAST
                   ) AS placement
            FROM users u
            WHERE CAST(:filter AS VARCHAR) = 'ALL'
               OR (CAST(:filter AS VARCHAR) = 'CORE'
                   AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
               OR (CAST(:filter AS VARCHAR) = 'BATCH_REP'
                   AND u.club_role = 'BATCH_REPRESENTATIVE')
               OR (CAST(:filter AS VARCHAR) = 'STUDENTS'
                   AND (u.club_role = 'STUDENT' OR u.club_role IS NULL))
            ORDER BY CASE WHEN CAST(:platform AS VARCHAR) = 'LEETCODE'
                          THEN u.leetcode_rating ELSE u.rating END DESC NULLS LAST,
                     u.id ASC
            """,
            countQuery = """
            SELECT count(*)
            FROM users u
            WHERE CAST(:filter AS VARCHAR) = 'ALL'
               OR (CAST(:filter AS VARCHAR) = 'CORE'
                   AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
               OR (CAST(:filter AS VARCHAR) = 'BATCH_REP'
                   AND u.club_role = 'BATCH_REPRESENTATIVE')
               OR (CAST(:filter AS VARCHAR) = 'STUDENTS'
                   AND (u.club_role = 'STUDENT' OR u.club_role IS NULL))
            """,
            nativeQuery = true)
    Page<LeaderboardEntryProjection> findFilteredLeaderboardPage(
            @Param("platform") String platform,
            @Param("filter") String filter,
            Pageable pageable);

    /**
     * Case-insensitive keyword search matching user names or Codeforces handles.
     *
     * @param query optional name or Codeforces-handle fragment
     * @param pageable requested page and sort order
     * @return page of matching members
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.codeforcesHandle) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
}
