package com.cpclub.backend.sync.dto;

import com.cpclub.backend.sync.entity.SyncJob;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SyncHealthDto {
    private SyncJob job;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private int membersOk;
    private int membersFailed;
    private LocalDateTime lastSuccessfulRun;
    private boolean stale;
}
