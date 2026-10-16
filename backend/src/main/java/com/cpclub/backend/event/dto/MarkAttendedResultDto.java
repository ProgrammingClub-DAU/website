package com.cpclub.backend.event.dto;

/**
 * @param added   members newly put on the attendance record
 * @param skipped members who were already on it
 */
public record MarkAttendedResultDto(int added, int skipped) {
}
