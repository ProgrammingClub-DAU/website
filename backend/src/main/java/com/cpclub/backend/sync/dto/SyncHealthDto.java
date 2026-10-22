package com.cpclub.backend.sync.dto;

import com.cpclub.backend.sync.entity.SyncJob;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Health of one sync job. Every timestamp is UTC (D31).
 */
@Data
@Builder
public class SyncHealthDto {

    /**
     * RUNNING   -- a run is in progress right now.
     * HEALTHY   -- the last finished run had no failures and is recent enough.
     * DEGRADED  -- the last finished run succeeded for some members and failed for others.
     * FAILING   -- the last finished run succeeded for nobody.
     * STALE     -- nothing has succeeded within twice the job's interval.
     * NEVER_RUN -- no run has ever been recorded.
     */
    public enum Status { RUNNING, HEALTHY, DEGRADED, FAILING, STALE, NEVER_RUN }

    private SyncJob job;
    private Status status;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int membersOk;
    private int membersFailed;
    /** First failure message of the last finished run, if any. */
    private String errorSample;
    /** Start of the most recent finished run in which at least one unit succeeded. */
    private LocalDateTime lastSuccessfulRun;
    private boolean stale;
    private boolean running;
}
