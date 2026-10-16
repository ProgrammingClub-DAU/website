package com.cpclub.backend.mail.repository;

import com.cpclub.backend.mail.entity.ReminderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** The idempotency gate for reminder emails (D29). */
@Repository
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    /**
     * Claims the right to send one reminder. Returns 1 when this call inserted the
     * row and 0 when it already existed -- that is, when the reminder was already
     * sent, or claimed by a run that is still going.
     *
     * <p>The database's unique constraint is the arbiter, not a read-then-write in
     * Java, so two runs racing for the same reminder cannot both win.</p>
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO reminder_log (user_id, kind, ref_key, sent_at) "
            + "VALUES (:userId, :kind, :refKey, :sentAt) "
            + "ON CONFLICT (user_id, kind, ref_key) DO NOTHING", nativeQuery = true)
    int claim(@Param("userId") Long userId, @Param("kind") String kind, @Param("refKey") String refKey,
              @Param("sentAt") LocalDateTime sentAtUtc);
}
