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

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByCodeforcesHandle(String codeforcesHandle);

    Optional<User> findByCodeforcesHandle(String codeforcesHandle);

    List<User> findByCodeforcesHandleIsNotNull();

    @Query("SELECT u FROM User u ORDER BY u.rating DESC NULLS LAST")
    Page<User> findAllByOrderByRatingDescNullsLast(Pageable pageable);

    @Query("SELECT u FROM User u WHERE " +
           "(:query IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.codeforcesHandle) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);
}
