package com.cpclub.backend.user.repository;

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
     * Counts the number of users whose rating is strictly greater than the given rating.
     * Used for calculating standard competition ranking (ties get the same rank).
     *
     * @param rating the rating to compare against
     * @return the number of users strictly better
     */
    long countByRatingGreaterThan(Integer rating);

    /**
     * Counts the total number of members who have a non-null rating.
     * Used to assign a rank to unrated members (which are placed at the end).
     *
     * @return total count of rated members
     */
    long countByRatingIsNotNull();

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
