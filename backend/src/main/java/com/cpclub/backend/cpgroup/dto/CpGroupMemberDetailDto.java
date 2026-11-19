package com.cpclub.backend.cpgroup.dto;

import com.cpclub.backend.stats.dto.MemberActivityDto.DailySolves;
import com.cpclub.backend.stats.dto.MemberActivityDto.RecentSolve;

import java.time.LocalDateTime;
import java.util.List;

/**
 * One CP Practice Group student in full, for the admin's student page.
 *
 * @param notes          admins' private notes; null when there are none
 * @param notesUpdatedBy the name of the admin who last edited them
 * @param addedBy        the name of the admin who added the student
 * @param daily          the last 30 IST days, oldest first
 * @param contests       every contest on record, newest first
 * @param recentSolves   the latest Codeforces solves, newest first
 */
public record CpGroupMemberDetailDto(
        CpGroupMemberRowDto summary,
        String notes,
        LocalDateTime notesUpdatedAt,
        String notesUpdatedBy,
        String addedBy,
        List<DailySolves> daily,
        List<ContestEntry> contests,
        List<RecentSolve> recentSolves) {

    /** @param startedAt UTC */
    public record ContestEntry(String platform, String contestName, LocalDateTime startedAt, boolean rated,
                               Integer rank, Integer oldRating, Integer newRating) {}
}
