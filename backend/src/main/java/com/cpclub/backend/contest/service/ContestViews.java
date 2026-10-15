package com.cpclub.backend.contest.service;

import com.cpclub.backend.contest.client.CodeforcesAuthorizedClient;
import com.cpclub.backend.contest.dto.ClubContestSummaryDto;
import com.cpclub.backend.contest.dto.MemberRefDto;
import com.cpclub.backend.contest.entity.ClubContest;
import com.cpclub.backend.user.entity.User;

import java.time.LocalDateTime;

/** Mapping shared by the contest services. */
final class ContestViews {

    private ContestViews() {
    }

    static MemberRefDto member(User user) {
        return MemberRefDto.builder()
                .id(user.getId())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .codeforcesHandle(user.getCodeforcesHandle())
                .build();
    }

    static LocalDateTime endsAt(ClubContest contest) {
        return contest.getStartsAt().plusSeconds(contest.getDurationSeconds());
    }

    static ClubContestSummaryDto summary(ClubContest contest, long participantCount) {
        return ClubContestSummaryDto.builder()
                .id(contest.getId())
                .cfContestId(contest.getCfContestId())
                .kind(contest.getKind())
                .name(contest.getName())
                .startsAt(contest.getStartsAt())
                .endsAt(endsAt(contest))
                .durationSeconds(contest.getDurationSeconds())
                .seasonId(contest.getSeason() != null ? contest.getSeason().getId() : null)
                .seasonName(contest.getSeason() != null ? contest.getSeason().getName() : null)
                .lastSyncedAt(contest.getLastSyncedAt())
                .participantCount(participantCount)
                .url(contestUrl(contest.getCfContestId()))
                .build();
    }

    /** Gym contests live under /gym/; official rounds under /contest/. */
    static String contestUrl(int cfContestId) {
        return "https://codeforces.com/" + section(cfContestId) + "/" + cfContestId;
    }

    static String problemUrl(int cfContestId, String index) {
        return contestUrl(cfContestId) + "/problem/" + index;
    }

    private static String section(int cfContestId) {
        return CodeforcesAuthorizedClient.isGym(cfContestId) ? "gym" : "contest";
    }
}
