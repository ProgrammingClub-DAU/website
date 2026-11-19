package com.cpclub.backend.cpgroup.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** Request and response bodies for the CP Practice Group endpoints. */
public final class CpGroupRequests {

    private CpGroupRequests() {}

    /** Adds students to a batch of the group; a student already in it moves to this batch. */
    public record AddMembers(
            @NotEmpty @Size(max = 500) List<@NotNull Long> userIds,
            @NotNull @Min(2000) @Max(2100) Integer batchYear) {}

    /**
     * Changes a student's group batch, notes, or both. A field left out is kept;
     * empty notes clear them.
     */
    public record UpdateMember(
            @Min(2000) @Max(2100) Integer batchYear,
            @Size(max = 5000) String notes) {}

    /** @param added how many were new to the group (the rest changed batch) */
    public record AddResult(int added, int moved) {}

    /** What a student sees of their own membership. */
    public record MyMembership(boolean member, Integer batchYear, LocalDateTime since) {}
}
