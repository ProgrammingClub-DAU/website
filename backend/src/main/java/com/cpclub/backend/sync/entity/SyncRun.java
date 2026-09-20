package com.cpclub.backend.sync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Records every scheduled or admin-triggered sync run.
 * error_sample is truncated to 1000 chars; never a full stack trace.
 */
@Entity
@Table(name = "sync_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SyncJob job;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "members_ok", nullable = false)
    @Builder.Default
    private int membersOk = 0;

    @Column(name = "members_failed", nullable = false)
    @Builder.Default
    private int membersFailed = 0;

    @Column(name = "error_sample", length = 1000)
    private String errorSample;
}