package com.cpclub.backend.sync.repository;

import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncRunRepository extends JpaRepository<SyncRun, Long> {
    List<SyncRun> findByJobOrderByStartedAtDesc(SyncJob job, Pageable pageable);
    List<SyncRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
