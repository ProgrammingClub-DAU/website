package com.cpclub.backend.snapshot.dto;

import com.cpclub.backend.snapshot.entity.WeeklySnapshot;

import java.time.LocalDate;

/**
 * Immutable DTO record representing one point on a member's rating history chart.
 *
 * <p>The date is exposed as a {@link LocalDate} rather than the entity's full
 * timestamp: snapshots are written once a week, so the time of day carries no
 * meaning for the chart and only complicates the axis on the frontend.</p>
 *
 * @param date day the rating was recorded
 * @param rating rating held on that day
 */
public record RatingPointDto(
        LocalDate date,
        Integer rating
) {
    /**
     * Maps a stored snapshot into a chart point.
     *
     * @param snapshot persisted weekly snapshot
     * @return immutable point for the rating history response
     */
    public static RatingPointDto fromEntity(WeeklySnapshot snapshot) {
        return new RatingPointDto(
                snapshot.getRecordedAt().toLocalDate(),
                snapshot.getRating()
        );
    }
}
