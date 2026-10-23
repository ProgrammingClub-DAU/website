package com.cpclub.backend.contest.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.contest.dto.ClubContestCreateRequest;
import com.cpclub.backend.contest.entity.ClubContestKind;
import com.cpclub.backend.contest.repository.ClubContestRepository;
import com.cpclub.backend.user.entity.Role;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventContestLinkerTest {

    private ClubContestService clubContestService;
    private ClubContestRepository contestRepository;
    private EventContestLinker linker;

    @BeforeEach
    void setUp() {
        clubContestService = mock(ClubContestService.class);
        contestRepository = mock(ClubContestRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        User admin = new User("Admin", "admin@dau.ac.in", null, Role.ROLE_ADMIN);
        admin.setId(1L);
        when(userRepository.findByEmail("admin@dau.ac.in")).thenReturn(Optional.of(admin));
        linker = new EventContestLinker(clubContestService, contestRepository, userRepository);
    }

    @Test
    @DisplayName("Contest, gym and group links are recognised, with the right kind")
    void parsesLinks() {
        assertThat(EventContestLinker.parse("https://codeforces.com/contest/1843"))
                .contains(new EventContestLinker.ContestLink(1843, ClubContestKind.OFFICIAL));
        assertThat(EventContestLinker.parse("https://codeforces.com/gym/724781/standings"))
                .contains(new EventContestLinker.ContestLink(724781, ClubContestKind.GYM));
        assertThat(EventContestLinker.parse("codeforces.com/group/AbC12/contest/567890"))
                .contains(new EventContestLinker.ContestLink(567890, ClubContestKind.GROUP));
        assertThat(EventContestLinker.parse("https://leetcode.com/contest/weekly-contest-400")).isEmpty();
        assertThat(EventContestLinker.parse(null)).isEmpty();
    }

    @Test
    @DisplayName("Saving an event with a new contest link adds the club contest")
    void addsTheClubContest() {
        when(contestRepository.existsByCfContestId(724781)).thenReturn(false);

        String note = linker.linkIfNeeded("https://codeforces.com/gym/724781", "admin@dau.ac.in");

        ArgumentCaptor<ClubContestCreateRequest> request = ArgumentCaptor.forClass(ClubContestCreateRequest.class);
        verify(clubContestService).create(request.capture(), any());
        assertThat(request.getValue().getCfContestId()).isEqualTo(724781);
        assertThat(request.getValue().getKind()).isEqualTo(ClubContestKind.GYM);
        assertThat(note).contains("added to the Contests page");
    }

    @Test
    @DisplayName("A contest that is already a club contest is left alone")
    void skipsAnExistingContest() {
        when(contestRepository.existsByCfContestId(1843)).thenReturn(true);

        assertThat(linker.linkIfNeeded("https://codeforces.com/contest/1843", "admin@dau.ac.in")).isNull();
        verify(clubContestService, never()).create(any(), anyLong());
    }

    @Test
    @DisplayName("A Codeforces failure becomes a note for the admin, never an exception")
    void failureIsReportedNotThrown() {
        when(contestRepository.existsByCfContestId(724781)).thenReturn(false);
        when(clubContestService.create(any(), any())).thenThrow(new BadRequestException("Codeforces API key not configured"));

        String note = linker.linkIfNeeded("https://codeforces.com/gym/724781", "admin@dau.ac.in");

        assertThat(note).contains("could not be added").contains("API key not configured");
    }
}
