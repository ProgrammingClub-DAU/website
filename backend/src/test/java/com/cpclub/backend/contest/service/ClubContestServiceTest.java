package com.cpclub.backend.contest.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ConflictException;
import com.cpclub.backend.contest.client.CfStandings;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient.CodeforcesApiException;
import com.cpclub.backend.contest.dto.ClubContestCreateRequest;
import com.cpclub.backend.contest.dto.ClubContestSummaryDto;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.contest.entity.ClubContestKind;
import com.cpclub.backend.contest.entity.ClubContestProblem;
import com.cpclub.backend.contest.repository.ClubContestProblemRepository;
import com.cpclub.backend.contest.repository.ClubContestProblemResultRepository;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.contest.repository.ClubContestResultRepository;
import com.cpclub.backend.contest.repository.ClubSeasonRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubContestServiceTest {

    @Mock private ClubContestRepository contestRepository;
    @Mock private ClubContestProblemRepository problemRepository;
    @Mock private ClubContestResultRepository resultRepository;
    @Mock private ClubContestProblemResultRepository problemResultRepository;
    @Mock private ClubSeasonRepository seasonRepository;
    @Mock private UserRepository userRepository;
    @Mock private CodeforcesAuthorizedClient codeforces;
    @Mock private TransactionTemplate transaction;

    @InjectMocks
    private ClubContestService service;

    @Test
    @DisplayName("A mashup with no API key configured is refused before Codeforces is called")
    void mashupWithoutKey_is400() {
        when(codeforces.isConfigured()).thenReturn(false);

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.create(request(102345, ClubContestKind.MASHUP), 1L));

        assertEquals("Codeforces API key not configured", e.getMessage());
        verify(codeforces, never()).contestHeader(anyInt());
        verify(contestRepository, never()).save(any());
    }

    @Test
    @DisplayName("An official round needs no API key")
    void officialWithoutKey_isAllowed() {
        when(contestRepository.existsByCfContestId(1352)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin()));
        when(codeforces.contestHeader(1352)).thenReturn(header(1352, "Codeforces Round 640 (Div. 4)", "A", "B"));
        when(transaction.execute(any())).thenAnswer(i -> ((TransactionCallback<?>) i.getArgument(0)).doInTransaction(null));
        when(contestRepository.save(any(ClubContest.class))).thenAnswer(i -> {
            ClubContest c = i.getArgument(0);
            c.setId(9L);
            return c;
        });

        ClubContestSummaryDto created = service.create(request(1352, ClubContestKind.OFFICIAL), 1L);

        assertEquals("Codeforces Round 640 (Div. 4)", created.getName());
        assertEquals(LocalDateTime.of(2020, 5, 9, 14, 35), created.getStartsAt(), "epoch seconds stored as UTC (D31)");
        assertEquals("https://codeforces.com/contest/1352", created.getUrl());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ClubContestProblem>> problems = ArgumentCaptor.forClass(List.class);
        verify(problemRepository).saveAll(problems.capture());
        assertEquals(List.of("A", "B"), problems.getValue().stream().map(ClubContestProblem::getProblemIndex).toList());
    }

    @Test
    @DisplayName("Kind and id must agree: an official kind with a gym id is a typo")
    void officialKindWithGymId_is400() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.create(request(102345, ClubContestKind.OFFICIAL), 1L));

        assertTrue(e.getMessage().contains("looks like a gym contest"));
        verify(codeforces, never()).contestHeader(anyInt());
    }

    @Test
    @DisplayName("Kind and id must agree: a gym kind with an official id is a typo")
    void gymKindWithOfficialId_is400() {
        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.create(request(1352, ClubContestKind.GYM), 1L));

        assertTrue(e.getMessage().contains("looks like an official round"));
    }

    @Test
    @DisplayName("Adding the same Codeforces contest twice is a conflict")
    void duplicate_is409() {
        when(contestRepository.existsByCfContestId(1352)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.create(request(1352, ClubContestKind.OFFICIAL), 1L));
        verify(codeforces, never()).contestHeader(anyInt());
    }

    @Test
    @DisplayName("Codeforces' own explanation reaches the admin when the contest is not found")
    void codeforcesRefusal_isA400WithItsComment() {
        when(contestRepository.existsByCfContestId(9999)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin()));
        when(codeforces.contestHeader(9999))
                .thenThrow(new CodeforcesApiException("contestId: Contest with id 9999 not found"));

        BadRequestException e = assertThrows(BadRequestException.class,
                () -> service.create(request(9999, ClubContestKind.OFFICIAL), 1L));

        assertEquals("Codeforces: contestId: Contest with id 9999 not found", e.getMessage());
        verify(contestRepository, never()).save(any());
    }

    private static ClubContestCreateRequest request(int cfId, ClubContestKind kind) {
        return ClubContestCreateRequest.builder().cfContestId(cfId).kind(kind).build();
    }

    private static User admin() {
        return User.builder().id(1L).name("Admin").email("admin@dau.ac.in").build();
    }

    private static CfStandings header(int id, String name, String... indexes) {
        CfStandings s = new CfStandings();
        CfStandings.Contest c = new CfStandings.Contest();
        c.setId(id);
        c.setName(name);
        c.setStartTimeSeconds(1589034900L);
        c.setDurationSeconds(7200L);
        s.setContest(c);
        for (String index : indexes) {
            CfStandings.Problem p = new CfStandings.Problem();
            p.setIndex(index);
            p.setName("Problem " + index);
            s.getProblems().add(p);
        }
        return s;
    }
}
