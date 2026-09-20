package com.cpclub.backend.sync.service;

import com.cpclub.backend.sync.dto.SyncHealthDto;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncHealthServiceTest {

    @Mock
    private SyncRunRepository repository;

    @InjectMocks
    private SyncHealthService service;

    @Test
    void whenLastSuccessThreeIntervalsAgo_staleIsTrue() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        
        SyncRun failed = SyncRun.builder()
                .job(SyncJob.CF_SUBMISSIONS)
                .startedAt(now.minusHours(1))
                .errorSample("Failed")
                .build();
                
        SyncRun success = SyncRun.builder()
                .job(SyncJob.CF_SUBMISSIONS)
                .startedAt(now.minusHours(19)) // 6 hour interval * 3 = 18 hours
                .build();

        when(repository.findByJobOrderByStartedAtDesc(eq(SyncJob.CF_SUBMISSIONS), any(Pageable.class)))
                .thenReturn(List.of(failed, success));
                
        List<SyncHealthDto> health = service.getHealth();
        
        SyncHealthDto cfSubmissions = health.stream()
                .filter(h -> h.getJob() == SyncJob.CF_SUBMISSIONS)
                .findFirst()
                .orElseThrow();
                
        assertTrue(cfSubmissions.isStale());
    }
}
