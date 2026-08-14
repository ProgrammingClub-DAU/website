package com.cpclub.backend.user.repository;

import com.cpclub.backend.leaderboard.dto.LeaderboardEntryProjection;
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
     * Resolves paginated list of users ordered by rating descending.
     * Non-rated members (null ratings) are pushed to the end of the ranking list.
     *
     * @param pageable requested page and size
     * @return page of members in ranking order
     */
    @Query("SELECT u FROM User u ORDER BY u.rating DESC NULLS LAST, u.id ASC")
    Page<User> findAllByOrderByRatingDescNullsLast(Pageable pageable);

    /**
     * Resolves one page of the leaderboard with each member's absolute rank already
     * computed by the database.
     *
     * <p>{@code RANK()} implements standard competition ranking: tied ratings share
     * a position and the next distinct rating skips the gap (1, 1, 3). Unrated
     * members sort last and, being all equal under {@code NULLS LAST}, tie with each
     * other on a single trailing rank.</p>
     *
     * <p>The window function is evaluated over the whole table before {@code LIMIT}
     * is applied, so ranks stay absolute across pages — page 2 continues from where
     * page 1 stopped rather than restarting at 1.</p>
     *
     * <p>This replaces a per-row {@code COUNT}, which cost one query per member on
     * every page load. Column aliases are deliberately single lowercase words; see
     * {@link com.cpclub.backend.leaderboard.dto.LeaderboardEntryProjection}.</p>
     *
     * @param pageable requested page and size
     * @return one page of ranked members
     */
    @Query(value = """
            SELECT u.id                AS id,
                   u.name              AS name,
                   u.codeforces_handle AS handle,
                   u.rating            AS rating,
                   RANK() OVER (ORDER BY u.rating DESC NULLS LAST) AS placement
            FROM users u
            ORDER BY u.rating DESC NULLS LAST, u.id ASC
            """,
            countQuery = "SELECT count(*) FROM users",
            nativeQuery = true)
    Page<LeaderboardEntryProjection> findLeaderboardPage(Pageable pageable);

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
