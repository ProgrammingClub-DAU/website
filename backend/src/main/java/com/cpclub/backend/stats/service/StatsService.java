package com.cpclub.backend.stats.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import com.cpclub.backend.stats.dto.*;
import com.cpclub.backend.stats.repository.StatsQueryRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing statistics calculations, leaderboards, user stats, rating histories, and comparisons (Section 4.9).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final StatsQueryRepository statsQueryRepository;
    private final StatsPeriodResolver statsPeriodResolver;
    private final UserRepository userRepository;

    @Value("${cpclub.auth.require-email-verification:false}")
    private boolean requireEmailVerification;

    /**
     * Retrieves ranked statistics leaderboard with strict metric/platform/period validation (Section 4.9).
     */
    public StatsLeaderboardResponseDto getLeaderboard(
            StatsMetric metric,
            StatsPlatform platform,
            StatsPeriod period,
            LeaderboardFilter filter,
            int page,
            int size
    ) {
        validateLeaderboardParams(metric, platform, period);

        if (filter == null) {
            filter = LeaderboardFilter.ALL;
        }
        if (period == null) {
            period = StatsPeriod.ALL_TIME;
        }
        if (platform == null) {
            platform = (metric == StatsMetric.AVG_PROBLEM_RATING) ? StatsPlatform.CODEFORCES : StatsPlatform.OVERALL;
        }

        StatsPeriodResolved resolved = statsPeriodResolver.resolve(period);

        StatsQueryRepository.LeaderboardQueryResult result = statsQueryRepository.findLeaderboard(
                metric,
                platform,
                period,
                filter,
                resolved,
                requireEmailVerification,
                page,
                size
        );

        LocalDate trackingSince = null;
        if (platform == StatsPlatform.LEETCODE || platform == StatsPlatform.OVERALL) {
            trackingSince = statsQueryRepository.getLeetCodeOverallEarliestCapturedOn();
        }

        java.time.LocalDateTime scoreComputedAt = null;
        if (metric == StatsMetric.OVERALL_SCORE) {
            scoreComputedAt = statsQueryRepository.getMemberScoreComputedAt();
        }

        int totalPages = size > 0 ? (int) Math.ceil((double) result.totalElements() / size) : 0;

        return StatsLeaderboardResponseDto.builder()
                .metric(metric)
                .platform(platform)
                .period(period)
                .filter(filter)
                .page(page)
                .size(size)
                .totalElements(result.totalElements())
                .totalPages(totalPages)
                .trackingSince(trackingSince)
                .computedAt(scoreComputedAt)
                .entries(result.entries())
                .build();
    }

    private void validateLeaderboardParams(StatsMetric metric, StatsPlatform platform, StatsPeriod period) {
        if (metric == null) {
            throw new BadRequestException("metric is required; allowed metrics: [SOLVED, CONTESTS, AVG_PROBLEM_RATING, ATTENDANCE, OVERALL_SCORE]");
        }

        if (metric == StatsMetric.OVERALL_SCORE) {
            if (platform != null && platform != StatsPlatform.OVERALL) {
                throw new BadRequestException("OVERALL_SCORE is only supported for platform OVERALL; allowed platforms: [OVERALL]");
            }
            if (period != null && period != StatsPeriod.ALL_TIME) {
                throw new BadRequestException("OVERALL_SCORE is only supported for period ALL_TIME; allowed periods: [ALL_TIME]");
            }
        }

        if (metric == StatsMetric.AVG_PROBLEM_RATING) {
            if (platform != null && platform != StatsPlatform.CODEFORCES) {
                throw new BadRequestException("AVG_PROBLEM_RATING is only supported for platform CODEFORCES; allowed platforms: [CODEFORCES]");
            }
        }

        if (metric == StatsMetric.ATTENDANCE) {
            if (platform != null && platform != StatsPlatform.OVERALL) {
                throw new BadRequestException("ATTENDANCE is only supported for platform OVERALL; allowed platforms: [OVERALL]");
            }
            if (period == StatsPeriod.WEEK) {
                throw new BadRequestException("ATTENDANCE does not support period WEEK; allowed periods: [MONTH, YEAR, ALL_TIME]");
            }
        }

        if (metric == StatsMetric.SOLVED || metric == StatsMetric.CONTESTS) {
            if (platform != null && platform != StatsPlatform.CODEFORCES && platform != StatsPlatform.LEETCODE && platform != StatsPlatform.OVERALL) {
                throw new BadRequestException("Invalid platform for " + metric + "; allowed platforms: [CODEFORCES, LEETCODE, OVERALL]");
            }
        }
    }

    /**
     * Retrieves member stats across linked platforms and overall aggregate (Section 4.9).
     */
    public MemberStatsDto getMemberStats(Long userId, StatsPeriod period) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        checkMemberVisibility(user);

        if (period == null) {
            period = StatsPeriod.ALL_TIME;
        }

        StatsPeriodResolved resolved = statsPeriodResolver.resolve(period);

        List<PlatformStatsDto> platformStats = new ArrayList<>();
        int cfSolved = 0;
        int cfContests = 0;
        Integer lcSolved = null;
        int lcContests = 0;

        if (user.getCodeforcesHandle() != null) {
            cfSolved = statsQueryRepository.countCodeforcesSolves(userId, resolved.startUtc(), resolved.endUtc());
            cfContests = statsQueryRepository.countContests(userId, Platform.CODEFORCES, resolved.startUtc(), resolved.endUtc());
            StatsQueryRepository.RatedSolvesResult ratedSolves = statsQueryRepository.getCodeforcesAvgRating(userId, resolved.startUtc(), resolved.endUtc());

            Double avgRating = ratedSolves.ratedCount() >= 10 ? ratedSolves.avgRating() : null;

            platformStats.add(PlatformStatsDto.builder()
                    .platform(StatsPlatform.CODEFORCES)
                    .handle(user.getCodeforcesHandle())
                    .rating(user.getRating())
                    .maxRating(user.getMaxRating())
                    .solved(cfSolved)
                    .contests(cfContests)
                    .avgProblemRating(avgRating)
                    .ratedSolvesCount(ratedSolves.ratedCount())
                    .lastSyncedAt(user.getCfSyncedAt())
                    .build());
        }

        if (user.getLeetcodeHandle() != null) {
            lcContests = statsQueryRepository.countContests(userId, Platform.LEETCODE, resolved.startUtc(), resolved.endUtc());
            LocalDate trackingSince = statsQueryRepository.getLeetCodeEarliestCapturedOn(userId);

            LeetCodeDifficultyDto lcDiff;
            if (period == StatsPeriod.ALL_TIME) {
                lcSolved = user.getLeetcodeTotalSolved();
                lcDiff = LeetCodeDifficultyDto.builder()
                        .easy(user.getLeetcodeEasySolved())
                        .medium(user.getLeetcodeMediumSolved())
                        .hard(user.getLeetcodeHardSolved())
                        .build();
            } else {
                StatsQueryRepository.BaselineResult baseline = statsQueryRepository.getLeetCodeBaseline(userId, resolved.startDateIst());
                if (baseline != null) {
                    lcSolved = user.getLeetcodeTotalSolved() != null
                            ? Math.max(0, user.getLeetcodeTotalSolved() - baseline.totalSolved())
                            : null;
                    Integer easy = user.getLeetcodeEasySolved() != null && baseline.easySolved() != null
                            ? Math.max(0, user.getLeetcodeEasySolved() - baseline.easySolved())
                            : null;
                    Integer medium = user.getLeetcodeMediumSolved() != null && baseline.mediumSolved() != null
                            ? Math.max(0, user.getLeetcodeMediumSolved() - baseline.mediumSolved())
                            : null;
                    Integer hard = user.getLeetcodeHardSolved() != null && baseline.hardSolved() != null
                            ? Math.max(0, user.getLeetcodeHardSolved() - baseline.hardSolved())
                            : null;
                    lcDiff = LeetCodeDifficultyDto.builder()
                            .easy(easy)
                            .medium(medium)
                            .hard(hard)
                            .build();
                } else {
                    lcSolved = null;
                    lcDiff = null;
                }
            }

            platformStats.add(PlatformStatsDto.builder()
                    .platform(StatsPlatform.LEETCODE)
                    .handle(user.getLeetcodeHandle())
                    .rating(user.getLeetcodeRating())
                    .solved(lcSolved)
                    .contests(lcContests)
                    .lcDifficulty(lcDiff)
                    .trackingSince(trackingSince)
                    .lastSyncedAt(user.getLeetcodeSyncedAt())
                    .build());
        }

        Integer overallSolved;
        boolean solvedPartial = false;
        if (user.getCodeforcesHandle() != null && user.getLeetcodeHandle() != null) {
            if (lcSolved == null) {
                overallSolved = cfSolved;
                solvedPartial = true;
            } else {
                overallSolved = cfSolved + lcSolved;
            }
        } else if (user.getCodeforcesHandle() != null) {
            overallSolved = cfSolved;
        } else if (user.getLeetcodeHandle() != null) {
            overallSolved = lcSolved;
            solvedPartial = (lcSolved == null);
        } else {
            overallSolved = 0;
        }

        int totalContests = cfContests + lcContests;

        OverallStatsDto overall = OverallStatsDto.builder()
                .solved(overallSolved)
                .solvedPartial(solvedPartial)
                .contests(totalContests)
                .build();

        MemberScoreDto memberScore = statsQueryRepository.getMemberScore(user.getId());

        PlatformStatsDto cfPlatform = platformStats.stream()
                .filter(p -> p.getPlatform() == StatsPlatform.CODEFORCES)
                .findFirst().orElse(null);
        PlatformStatsDto lcPlatform = platformStats.stream()
                .filter(p -> p.getPlatform() == StatsPlatform.LEETCODE)
                .findFirst().orElse(null);

        return MemberStatsDto.builder()
                .userId(user.getId())
                .name(user.getName())
                .period(period)
                .platforms(platformStats)
                .codeforces(cfPlatform)
                .leetcode(lcPlatform)
                .overall(overall)
                .overallScore(memberScore)
                .build();
    }

    private void checkMemberVisibility(User user) {
        if (requireEmailVerification && !user.isEmailVerified()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean allowed = false;
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                String callerEmail = auth.getName();
                boolean isAdmin = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                if (callerEmail.equalsIgnoreCase(user.getEmail()) || isAdmin) {
                    allowed = true;
                }
            }
            if (!allowed) {
                throw new ResourceNotFoundException("User not found with id: " + user.getId());
            }
        }
    }

    /**
     * Retrieves contest rating progression history for Codeforces or LeetCode (O10, Section 4.9).
     */
    public List<RatingPointDto> getRatingHistory(Long userId, StatsPlatform platform) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        checkMemberVisibility(user);

        if (platform == null || (platform != StatsPlatform.CODEFORCES && platform != StatsPlatform.LEETCODE)) {
            throw new BadRequestException("platform must be CODEFORCES or LEETCODE; received: " + platform);
        }

        Platform domainPlatform = Platform.valueOf(platform.name());
        return statsQueryRepository.getRatingHistory(userId, domainPlatform);
    }

    /**
     * Compares two members, including ALL_TIME & YEAR stats, rating histories, and head-to-head records (P6, D25).
     */
    public CompareDto compare(Long aId, Long bId) {
        if (aId == null || bId == null) {
            throw new BadRequestException("Both 'a' and 'b' user IDs are required for comparison");
        }

        User userA = userRepository.findById(aId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + aId));
        User userB = userRepository.findById(bId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + bId));

        checkMemberVisibility(userA);
        checkMemberVisibility(userB);

        CompareUserSummaryDto summaryA = CompareUserSummaryDto.builder()
                .id(userA.getId())
                .name(userA.getName())
                .codeforcesHandle(userA.getCodeforcesHandle())
                .leetcodeHandle(userA.getLeetcodeHandle())
                .avatarUrl(userA.getAvatarUrl())
                .clubRole(userA.getClubRole())
                .build();

        CompareUserSummaryDto summaryB = CompareUserSummaryDto.builder()
                .id(userB.getId())
                .name(userB.getName())
                .codeforcesHandle(userB.getCodeforcesHandle())
                .leetcodeHandle(userB.getLeetcodeHandle())
                .avatarUrl(userB.getAvatarUrl())
                .clubRole(userB.getClubRole())
                .build();

        MemberStatsDto statsAAllTime = getMemberStats(aId, StatsPeriod.ALL_TIME);
        MemberStatsDto statsBAllTime = getMemberStats(bId, StatsPeriod.ALL_TIME);
        MemberStatsDto statsAYear = getMemberStats(aId, StatsPeriod.YEAR);
        MemberStatsDto statsBYear = getMemberStats(bId, StatsPeriod.YEAR);

        List<RatingPointDto> cfHistoryA = statsQueryRepository.getRatingHistory(aId, Platform.CODEFORCES);
        List<RatingPointDto> cfHistoryB = statsQueryRepository.getRatingHistory(bId, Platform.CODEFORCES);
        List<RatingPointDto> lcHistoryA = statsQueryRepository.getRatingHistory(aId, Platform.LEETCODE);
        List<RatingPointDto> lcHistoryB = statsQueryRepository.getRatingHistory(bId, Platform.LEETCODE);

        List<HeadToHeadContestDto> sharedContests = statsQueryRepository.getSharedCodeforcesContests(aId, bId);

        int aAhead = 0;
        int bAhead = 0;
        int tied = 0;
        for (HeadToHeadContestDto c : sharedContests) {
            if (c.getRankA() != null && c.getRankB() != null) {
                if (c.getRankA() < c.getRankB()) {
                    aAhead++;
                } else if (c.getRankB() < c.getRankA()) {
                    bAhead++;
                } else {
                    tied++;
                }
            }
        }

        List<HeadToHeadContestDto> recent = sharedContests.size() > 10
                ? sharedContests.subList(0, 10)
                : sharedContests;

        HeadToHeadDto h2h = HeadToHeadDto.builder()
                .aAhead(aAhead)
                .bAhead(bAhead)
                .tied(tied)
                .recentContests(recent)
                .build();

        return CompareDto.builder()
                .userA(summaryA)
                .userB(summaryB)
                .statsAAllTime(statsAAllTime)
                .statsBAllTime(statsBAllTime)
                .statsAYear(statsAYear)
                .statsBYear(statsBYear)
                .ratingHistoryACodeforces(cfHistoryA)
                .ratingHistoryBCodeforces(cfHistoryB)
                .ratingHistoryALeetcode(lcHistoryA)
                .ratingHistoryBLeetcode(lcHistoryB)
                .headToHead(h2h)
                .build();
    }
}
