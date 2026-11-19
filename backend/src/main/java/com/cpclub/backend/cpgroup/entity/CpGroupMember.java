package com.cpclub.backend.cpgroup.entity;

import com.cpclub.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A student in the CP Practice Group (V32). Admin-only: the student sees just
 * that they are a member, on their own profile.
 */
@Entity
@Table(name = "cp_group_members")
@Getter
@Setter
@NoArgsConstructor
public class CpGroupMember {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    /** The group's batch, set by an admin; not the batch on the student's profile. */
    @Column(name = "batch_year", nullable = false)
    private Integer batchYear;

    /** UTC (D31). */
    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    /** The admin who added them; null once that account is gone. */
    @Column(name = "added_by")
    private Long addedBy;

    /** Admins' private notes on the student. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** UTC (D31). */
    @Column(name = "notes_updated_at")
    private LocalDateTime notesUpdatedAt;

    @Column(name = "notes_updated_by")
    private Long notesUpdatedBy;

    public CpGroupMember(User user, int batchYear, LocalDateTime addedAt, Long addedBy) {
        this.user = user;
        this.batchYear = batchYear;
        this.addedAt = addedAt;
        this.addedBy = addedBy;
    }
}
