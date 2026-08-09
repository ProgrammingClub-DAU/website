package com.cpclub.backend.repository;

import com.cpclub.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCodeforcesHandle(String codeforcesHandle);

    boolean existsByEmail(String email);

    List<User> findAllByOrderByRatingDesc();
}
