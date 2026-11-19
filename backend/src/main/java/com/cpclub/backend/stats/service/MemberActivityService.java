package com.cpclub.backend.stats.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.stats.dto.MemberActivityDto;
import com.cpclub.backend.stats.dto.MemberActivityDto.DailySolves;
import com.cpclub.backend.stats.dto.MemberActivityDto.RecentSolve;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A member's recent activity, shown on their profile and on the admin's CP
 * Practice Group page: problems solved per day over the last 30 days, and their
 * latest Codeforces solves.
 */
@Service
@RequiredArgsConstructor
public class MemberActivityService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    static final int DAYS = 30;
    static final int RECENT_SOLVES = 15;

    private final NamedParameterJdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MemberActivityDto activityOf(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return new MemberActivityDto(dailySolves(userId), recentSolves(userId));
    }

    /** The last 30 IST days, oldest first. */
    public List<DailySolves> dailySolves(Long userId) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, IST);
        LocalDate firstDay = today.minusDays(DAYS - 1);

        Map<LocalDate, Integer> codeforces = new HashMap<>();
        jdbc.query("""
                SELECT s.first_ac_at
                FROM cf_solves s JOIN cf_problems p ON p.id = s.problem_id
                WHERE s.user_id = :userId AND s.first_ac_at >= :from
                  AND (p.contest_id IS NULL OR p.contest_id < 100000)
                """,
                new MapSqlParameterSource("userId", userId)
                        .addValue("from", LocalDateTime.ofInstant(now, ZoneOffset.UTC).minusDays(DAYS + 1)),
                rs -> {
                    LocalDateTime at = rs.getObject("first_ac_at", LocalDateTime.class);
                    codeforces.merge(at.atOffset(ZoneOffset.UTC).atZoneSameInstant(IST).toLocalDate(), 1, Integer::sum);
                });

        Map<LocalDate, Integer> leetcodeTotals = new HashMap<>();
        jdbc.query("""
                SELECT captured_on, total_solved FROM platform_daily_totals
                WHERE platform = 'LEETCODE' AND user_id = :userId AND captured_on >= :from
                """,
                new MapSqlParameterSource("userId", userId).addValue("from", firstDay.minusDays(1)),
                rs -> { leetcodeTotals.put(rs.getObject("captured_on", LocalDate.class), rs.getInt("total_solved")); });

        List<DailySolves> daily = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            Integer total = leetcodeTotals.get(day);
            Integer before = leetcodeTotals.get(day.minusDays(1));
            Integer leetcode = total != null && before != null ? Math.max(0, total - before) : null;
            daily.add(new DailySolves(day, codeforces.getOrDefault(day, 0), leetcode));
        }
        return daily;
    }

    /** The latest Codeforces solves, newest first, gym included (with a gym link). */
    public List<RecentSolve> recentSolves(Long userId) {
        return jdbc.query("""
                SELECT p.problem_key, p.name, p.rating, p.contest_id, p.problem_index, s.first_ac_at
                FROM cf_solves s JOIN cf_problems p ON p.id = s.problem_id
                WHERE s.user_id = :userId
                ORDER BY s.first_ac_at DESC
                """ + " LIMIT " + RECENT_SOLVES,
                new MapSqlParameterSource("userId", userId),
                (rs, i) -> new RecentSolve(
                        rs.getString("problem_key"),
                        rs.getString("name"),
                        rs.getObject("rating", Integer.class),
                        rs.getObject("first_ac_at", LocalDateTime.class),
                        problemUrl(rs.getObject("contest_id", Integer.class), rs.getString("problem_index"))));
    }

    private static String problemUrl(Integer contestId, String index) {
        if (contestId == null) return null;
        return contestId < 100000
                ? "https://codeforces.com/problemset/problem/" + contestId + "/" + index
                : "https://codeforces.com/gym/" + contestId + "/problem/" + index;
    }
}
