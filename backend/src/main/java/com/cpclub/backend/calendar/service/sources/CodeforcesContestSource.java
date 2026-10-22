package com.cpclub.backend.calendar.service.sources;

import com.cpclub.backend.calendar.dto.ExternalContestDto;
import com.cpclub.backend.calendar.entity.ContestPlatform;
import com.cpclub.backend.codeforces.client.CodeforcesApiClient;
import com.cpclub.backend.codeforces.dto.CfContest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CodeforcesContestSource implements ExternalContestSource {

    private final CodeforcesApiClient apiClient;

    @Override
    public ContestPlatform platform() {
        return ContestPlatform.CODEFORCES;
    }

    @Override
    public List<ExternalContestDto> fetchUpcoming() {
        List<ExternalContestDto> upcoming = new ArrayList<>();

        for (CfContest contest : apiClient.contestList(false)) {
            // Unscheduled rounds are listed with no start time; there is nothing to put on a calendar.
            if (!"BEFORE".equals(contest.getPhase()) || contest.getId() == null || contest.getStartTimeSeconds() == null) {
                continue;
            }
            upcoming.add(ExternalContestDto.builder()
                    .id(String.valueOf(contest.getId()))
                    .platform(ContestPlatform.CODEFORCES)
                    .name(contest.getName())
                    .url("https://codeforces.com/contest/" + contest.getId())
                    .startsAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(contest.getStartTimeSeconds()), ZoneOffset.UTC))
                    .durationSeconds(contest.getDurationSeconds() != null ? contest.getDurationSeconds() : 0)
                    .build());
        }

        return upcoming;
    }
}
