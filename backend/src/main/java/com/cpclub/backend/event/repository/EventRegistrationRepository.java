package com.cpclub.backend.event.repository;

import com.cpclub.backend.event.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/** Members' own RSVPs (D13). Attendance is a separate, admin-only record. */
@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    long countByEventId(Long eventId);

    @Modifying
    @Query("DELETE FROM EventRegistration r WHERE r.event.id = :eventId AND r.user.id = :userId")
    int deleteByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);

    @Query("SELECT r FROM EventRegistration r JOIN FETCH r.user WHERE r.event.id = :eventId ORDER BY r.registeredAt ASC")
    List<EventRegistration> findByEventIdWithUser(@Param("eventId") Long eventId);

    @Query("SELECT r.user.id FROM EventRegistration r WHERE r.event.id = :eventId")
    Set<Long> findUserIdsByEventId(@Param("eventId") Long eventId);

    /**
     * Bulk delete for a forced event delete. Explicit rather than left to the
     * migration's ON DELETE CASCADE, which the H2 test schema does not have.
     */
    @Modifying
    @Query("DELETE FROM EventRegistration r WHERE r.event.id = :eventId")
    int deleteAllByEventId(@Param("eventId") Long eventId);
}
