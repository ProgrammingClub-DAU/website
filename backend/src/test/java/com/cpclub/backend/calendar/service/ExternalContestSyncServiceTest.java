package com.cpclub.backend.calendar.service;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.calendar.entity.ExternalContest;
import com.cpclub.backend.calendar.repository.ExternalContestRepository;
import com.cpclub.backend.calendar.service.sources.AtCoderContestSource;
import com.cpclub.backend.calendar.service.sources.ClistContestSource;
import com.cpclub.backend.calendar.service.sources.CodeforcesContestSource;
import com.cpclub.backend.calendar.service.sources.ExternalContestSource;
import com.cpclub.backend.sync.entity.SyncJob;
import com.cpclub.backend.sync.entity.SyncRun;
import com.cpclub.backend.sync.repository.SyncRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalContestSyncServiceTest {

    @Mock
    private ExternalContestRepository repository;
    
    @Mock
    private SyncRunRepository syncRunRepository;

    @Mock
    private AtCoderContestSource atCoderContestSource;
    
    @Mock
    private CodeforcesContestSource codeforcesContestSource;
    
    @Mock
    private ClistContestSource clistContestSource;

    private ExternalContestSyncService syncService;

    @BeforeEach
    void setUp() {
        when(atCoderContestSource.platform()).thenReturn(ContestPlatform.ATCODER);
        when(codeforcesContestSource.platform()).thenReturn(ContestPlatform.CODEFORCES);
        
        List<ExternalContestSource> sources = List.of(atCoderContestSource, codeforcesContestSource);
        syncService = new ExternalContestSyncService(sources, clistContestSource, repository, syncRunRepository);
        ReflectionTestUtils.setField(syncService, "enabled", true);
        
        when(clistContestSource.isConfigured()).thenReturn(false);
    }

    @Test
    void whenAtCoderSourceThrows_existingRowsKept() {
        when(syncRunRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(atCoderContestSource.fetchUpcoming()).thenThrow(new RuntimeException("Scraping failed"));
        
        syncService.sync();
        
        verify(repository, never()).delete(any());
    }

    @Test
    void whenSuccessfulCodeforcesFetchNoLongerListsFutureContest_thatRowDeleted() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        ExternalContest existingFuture = ExternalContest.builder()
                .platform(ContestPlatform.CODEFORCES)
                .externalId("1001")
                .startsAt(now.plusDays(2))
                .build();
                
        when(repository.findByPlatform(ContestPlatform.CODEFORCES)).thenReturn(List.of(existingFuture));
        when(codeforcesContestSource.fetchUpcoming()).thenReturn(List.of());
        
        syncService.syncPlatform(ContestPlatform.CODEFORCES);
        
        verify(repository).delete(existingFuture);
    }
}
