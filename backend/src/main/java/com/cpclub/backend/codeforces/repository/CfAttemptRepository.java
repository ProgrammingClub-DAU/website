package com.cpclub.backend.codeforces.repository;

import com.cpclub.backend.codeforces.entity.CfAttempt;
import com.cpclub.backend.codeforces.entity.CfAttemptId;
import com.cpclub.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CfAttemptRepository extends JpaRepository<CfAttempt, CfAttemptId> {

    /** On a Codeforces handle change: the old handle's attempts are not this member's. */
    @Modifying
    @Query("DELETE FROM CfAttempt a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);
}
