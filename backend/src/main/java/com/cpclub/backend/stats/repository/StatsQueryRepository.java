package com.cpclub.backend.stats.repository;

import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.leaderboard.dto.LeaderboardFilter;
import com.cpclub.backend.stats.dto.*;
import com.cpclub.backend.user.entity.ClubRole;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Native SQL query repository backing the statistics query layer (Section 1.1, 4.8).
 * Executes dynamic aggregations and leaderboard ranking using NamedParameterJdbcTemplate.
 */
@Repository
@RequiredArgsConstructor
public class StatsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public record RatedSolvesResult(Double avgRating, int ratedCount) {}
    public record BaselineResult(int totalSolved, Integer easySolved, Integer mediumSolved, Integer hardSolved) {}
    public record LeaderboardQueryResult(List<StatsLeaderboardEntryDto> entries, long totalElements) {}

    private static final RowMapper<StatsLeaderboardEntryDto> LEADERBOARD_ROW_MAPPER = (rs, rowNum) -> {
        String roleStr = rs.getString("club_role");
        ClubRole role = null;
        if (roleStr != null) {
            try {
                role = ClubRole.valueOf(roleStr);
            } catch (IllegalArgumentException ignored) {}
        }

        Double val = rs.getObject("metric_value") != null ? rs.getDouble("metric_value") : null;
        Integer basis = rs.getObject("basis_val") != null ? rs.getInt("basis_val") : null;

        return StatsLeaderboardEntryDto.builder()
                .rank(rs.getInt("placement"))
                .userId(rs.getLong("user_id"))
                .name(rs.getString("name"))
                .codeforcesHandle(rs.getString("codeforces_handle"))
                .leetcodeHandle(rs.getString("leetcode_handle"))
                .avatarUrl(rs.getString("avatar_url"))
                .clubRole(role)
                .value(val)
                .basis(basis)
                .build();
    };

    /**
     * Executes the ranked statistics leaderboard query for a chosen metric, platform, period, and filter.
     */
    public LeaderboardQueryResult findLeaderboard(
            StatsMetric metric,
            StatsPlatform platform,
            StatsPeriod period,
            LeaderboardFilter filter,
            StatsPeriodResolved resolved,
            boolean requireEmailVerification,
            int page,
            int size
    ) {
        String cteSql = buildLeaderboardCte(metric, platform, period);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("filter", filter.name())
                .addValue("requireEmailVerification", requireEmailVerification)
                .addValue("startUtc", Timestamp.from(resolved.startUtc()))
                .addValue("endUtc", Timestamp.from(resolved.endUtc()))
                .addValue("startDateIst", Date.valueOf(resolved.startDateIst()))
                .addValue("platform", platform.name())
                .addValue("offset", (long) page * size)
                .addValue("limit", size);

        String countSql = cteSql + " SELECT COUNT(*) FROM ranked";
        Long totalElements = jdbcTemplate.queryForObject(countSql, params, Long.class);
        if (totalElements == null || totalElements == 0) {
            return new LeaderboardQueryResult(List.of(), 0);
        }

        String pageSql = cteSql + " SELECT * FROM ranked ORDER BY placement ASC, name ASC, user_id ASC LIMIT :limit OFFSET :offset";
        List<StatsLeaderboardEntryDto> entries = jdbcTemplate.query(pageSql, params, LEADERBOARD_ROW_MAPPER);

        return new LeaderboardQueryResult(entries, totalElements);
    }

    private String buildLeaderboardCte(StatsMetric metric, StatsPlatform platform, StatsPeriod period) {
        return switch (metric) {
            case SOLVED -> buildSolvedCte(platform, period);
            case CONTESTS -> buildContestsCte();
            case AVG_PROBLEM_RATING -> buildAvgProblemRatingCte();
            case ATTENDANCE -> buildAttendanceCte();
            case OVERALL_SCORE -> buildOverallScoreCte();
        };
    }

    private String buildSolvedCte(StatsPlatform platform, StatsPeriod period) {
        if (platform == StatsPlatform.CODEFORCES) {
            return """
                WITH filtered_users AS (
                    SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role
                    FROM users u
                    WHERE (:filter = 'ALL'
                       OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                       OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                       OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                      AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                      AND u.codeforces_handle IS NOT NULL
                ),
                user_solves AS (
                    SELECT fu.id AS user_id,
                           fu.name,
                           fu.codeforces_handle,
                           fu.leetcode_handle,
                           fu.avatar_url,
                           fu.club_role,
                           COUNT(DISTINCT s.problem_id) AS metric_value
                    FROM filtered_users fu
                    JOIN cf_solves s ON fu.id = s.user_id
                    JOIN cf_problems p ON s.problem_id = p.id
                    WHERE p.contest_id < 100000
                      AND s.first_ac_at >= :startUtc
                      AND s.first_ac_at < :endUtc
                    GROUP BY fu.id, fu.name, fu.codeforces_handle, fu.leetcode_handle, fu.avatar_url, fu.club_role
                    HAVING COUNT(DISTINCT s.problem_id) > 0
                ),
                ranked AS (
                    SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                           metric_value,
                           CAST(NULL AS INTEGER) AS basis_val,
                           RANK() OVER (ORDER BY metric_value DESC) AS placement
                    FROM user_solves
                )
                """;
        } else if (platform == StatsPlatform.LEETCODE) {
            if (period == StatsPeriod.ALL_TIME) {
                return """
                    WITH filtered_users AS (
                        SELECT u.id AS user_id,
                               u.name,
                               u.codeforces_handle,
                               u.leetcode_handle,
                               u.avatar_url,
                               u.club_role,
                               u.leetcode_total_solved AS metric_value
                        FROM users u
                        WHERE (:filter = 'ALL'
                           OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                           OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                           OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                          AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                          AND u.leetcode_handle IS NOT NULL
                          AND u.leetcode_total_solved IS NOT NULL
                          AND u.leetcode_total_solved > 0
                    ),
                    ranked AS (
                        SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                               metric_value,
                               CAST(NULL AS INTEGER) AS basis_val,
                               RANK() OVER (ORDER BY metric_value DESC) AS placement
                        FROM filtered_users
                    )
                    """;
            } else {
                return """
                    WITH filtered_users AS (
                        SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role, u.leetcode_total_solved
                        FROM users u
                        WHERE (:filter = 'ALL'
                           OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                           OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                           OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                          AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                          AND u.leetcode_handle IS NOT NULL
                          AND u.leetcode_total_solved IS NOT NULL
                    ),
                    baselines AS (
                        SELECT pdt.user_id,
                               pdt.total_solved AS baseline_solved,
                               ROW_NUMBER() OVER (PARTITION BY pdt.user_id ORDER BY pdt.captured_on DESC) AS rn
                        FROM platform_daily_totals pdt
                        WHERE pdt.platform = 'LEETCODE'
                          AND pdt.captured_on <= :startDateIst
                    ),
                    latest_baselines AS (
                        SELECT user_id, baseline_solved
                        FROM baselines
                        WHERE rn = 1
                    ),
                    user_diffs AS (
                        SELECT fu.id AS user_id,
                               fu.name,
                               fu.codeforces_handle,
                               fu.leetcode_handle,
                               fu.avatar_url,
                               fu.club_role,
                               (fu.leetcode_total_solved - lb.baseline_solved) AS metric_value
                        FROM filtered_users fu
                        JOIN latest_baselines lb ON fu.id = lb.user_id
                        WHERE (fu.leetcode_total_solved - lb.baseline_solved) > 0
                    ),
                    ranked AS (
                        SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                               metric_value,
                               CAST(NULL AS INTEGER) AS basis_val,
                               RANK() OVER (ORDER BY metric_value DESC) AS placement
                        FROM user_diffs
                    )
                    """;
            }
        } else {
            // OVERALL
            if (period == StatsPeriod.ALL_TIME) {
                return """
                    WITH filtered_users AS (
                        SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role,
                               COALESCE(u.leetcode_total_solved, 0) AS lc_val
                        FROM users u
                        WHERE (:filter = 'ALL'
                           OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                           OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                           OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                          AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                    ),
                    cf_counts AS (
                        SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS cf_val
                        FROM cf_solves s
                        JOIN cf_problems p ON s.problem_id = p.id
                        WHERE p.contest_id < 100000
                          AND s.first_ac_at >= :startUtc
                          AND s.first_ac_at < :endUtc
                        GROUP BY s.user_id
                    ),
                    user_totals AS (
                        SELECT fu.id AS user_id,
                               fu.name,
                               fu.codeforces_handle,
                               fu.leetcode_handle,
                               fu.avatar_url,
                               fu.club_role,
                               (COALESCE(c.cf_val, 0) + fu.lc_val) AS metric_value
                        FROM filtered_users fu
                        LEFT JOIN cf_counts c ON fu.id = c.user_id
                        WHERE (COALESCE(c.cf_val, 0) + fu.lc_val) > 0
                    ),
                    ranked AS (
                        SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                               metric_value,
                               CAST(NULL AS INTEGER) AS basis_val,
                               RANK() OVER (ORDER BY metric_value DESC) AS placement
                        FROM user_totals
                    )
                    """;
            } else {
                return """
                    WITH filtered_users AS (
                        SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role, u.leetcode_total_solved
                        FROM users u
                        WHERE (:filter = 'ALL'
                           OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                           OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                           OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                          AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                    ),
                    cf_counts AS (
                        SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS cf_val
                        FROM cf_solves s
                        JOIN cf_problems p ON s.problem_id = p.id
                        WHERE p.contest_id < 100000
                          AND s.first_ac_at >= :startUtc
                          AND s.first_ac_at < :endUtc
                        GROUP BY s.user_id
                    ),
                    baselines AS (
                        SELECT pdt.user_id,
                               pdt.total_solved AS baseline_solved,
                               ROW_NUMBER() OVER (PARTITION BY pdt.user_id ORDER BY pdt.captured_on DESC) AS rn
                        FROM platform_daily_totals pdt
                        WHERE pdt.platform = 'LEETCODE'
                          AND pdt.captured_on <= :startDateIst
                    ),
                    latest_baselines AS (
                        SELECT user_id, baseline_solved
                        FROM baselines
                        WHERE rn = 1
                    ),
                    user_totals AS (
                        SELECT fu.id AS user_id,
                               fu.name,
                               fu.codeforces_handle,
                               fu.leetcode_handle,
                               fu.avatar_url,
                               fu.club_role,
                               (COALESCE(c.cf_val, 0) + CASE WHEN lb.baseline_solved IS NOT NULL AND fu.leetcode_total_solved IS NOT NULL
                                                             THEN GREATEST(0, fu.leetcode_total_solved - lb.baseline_solved)
                                                             ELSE 0 END) AS metric_value
                        FROM filtered_users fu
                        LEFT JOIN cf_counts c ON fu.id = c.user_id
                        LEFT JOIN latest_baselines lb ON fu.id = lb.user_id
                        WHERE (COALESCE(c.cf_val, 0) + CASE WHEN lb.baseline_solved IS NOT NULL AND fu.leetcode_total_solved IS NOT NULL
                                                            THEN GREATEST(0, fu.leetcode_total_solved - lb.baseline_solved)
                                                            ELSE 0 END) > 0
                    ),
                    ranked AS (
                        SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                               metric_value,
                               CAST(NULL AS INTEGER) AS basis_val,
                               RANK() OVER (ORDER BY metric_value DESC) AS placement
                        FROM user_totals
                    )
                    """;
            }
        }
    }

    private String buildContestsCte() {
        return """
            WITH filtered_users AS (
                SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role
                FROM users u
                WHERE (:filter = 'ALL'
                   OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                   OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                   OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                  AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                  AND (
                      (:platform = 'CODEFORCES' AND u.codeforces_handle IS NOT NULL) OR
                      (:platform = 'LEETCODE' AND u.leetcode_handle IS NOT NULL) OR
                      (:platform = 'OVERALL' AND (u.codeforces_handle IS NOT NULL OR u.leetcode_handle IS NOT NULL))
                  )
            ),
            user_contests AS (
                SELECT fu.id AS user_id,
                       fu.name,
                       fu.codeforces_handle,
                       fu.leetcode_handle,
                       fu.avatar_url,
                       fu.club_role,
                       COUNT(*) AS metric_value
                FROM filtered_users fu
                JOIN contest_participations cp ON fu.id = cp.user_id
                WHERE cp.started_at >= :startUtc
                  AND cp.started_at < :endUtc
                  AND (:platform = 'OVERALL' OR cp.platform = :platform)
                GROUP BY fu.id, fu.name, fu.codeforces_handle, fu.leetcode_handle, fu.avatar_url, fu.club_role
                HAVING COUNT(*) > 0
            ),
            ranked AS (
                SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                       metric_value,
                       CAST(NULL AS INTEGER) AS basis_val,
                       RANK() OVER (ORDER BY metric_value DESC) AS placement
                FROM user_contests
            )
            """;
    }

    private String buildAvgProblemRatingCte() {
        return """
            WITH filtered_users AS (
                SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role
                FROM users u
                WHERE (:filter = 'ALL'
                   OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                   OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                   OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                  AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
                  AND u.codeforces_handle IS NOT NULL
            ),
            user_avg AS (
                SELECT fu.id AS user_id,
                       fu.name,
                       fu.codeforces_handle,
                       fu.leetcode_handle,
                       fu.avatar_url,
                       fu.club_role,
                       ROUND(AVG(p.rating), 1) AS metric_value,
                       COUNT(p.rating) AS basis_val
                FROM filtered_users fu
                JOIN cf_solves s ON fu.id = s.user_id
                JOIN cf_problems p ON s.problem_id = p.id
                WHERE p.contest_id < 100000
                  AND p.rating IS NOT NULL
                  AND s.first_ac_at >= :startUtc
                  AND s.first_ac_at < :endUtc
                GROUP BY fu.id, fu.name, fu.codeforces_handle, fu.leetcode_handle, fu.avatar_url, fu.club_role
                HAVING COUNT(p.rating) >= 10
            ),
            ranked AS (
                SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                       metric_value,
                       basis_val,
                       RANK() OVER (ORDER BY metric_value DESC) AS placement
                FROM user_avg
            )
            """;
    }

    private String buildAttendanceCte() {
        return """
            WITH filtered_users AS (
                SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role
                FROM users u
                WHERE (:filter = 'ALL'
                   OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                   OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                   OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                  AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
            ),
            user_attendance AS (
                SELECT fu.id AS user_id,
                       fu.name,
                       fu.codeforces_handle,
                       fu.leetcode_handle,
                       fu.avatar_url,
                       fu.club_role,
                       COUNT(*) AS metric_value
                FROM filtered_users fu
                JOIN event_attendees ea ON fu.id = ea.user_id
                JOIN events e ON ea.event_id = e.id
                WHERE e.status = 'COMPLETED'
                  AND e.event_date >= :startUtc
                  AND e.event_date < :endUtc
                GROUP BY fu.id, fu.name, fu.codeforces_handle, fu.leetcode_handle, fu.avatar_url, fu.club_role
                HAVING COUNT(*) > 0
            ),
            ranked AS (
                SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                       metric_value,
                       CAST(NULL AS INTEGER) AS basis_val,
                       RANK() OVER (ORDER BY metric_value DESC) AS placement
                FROM user_attendance
            )
            """;
    }

    private String buildOverallScoreCte() {
        return """
            WITH filtered_users AS (
                SELECT u.id, u.name, u.codeforces_handle, u.leetcode_handle, u.avatar_url, u.club_role
                FROM users u
                WHERE (:filter = 'ALL'
                   OR (:filter = 'CORE' AND u.club_role IN ('CONVENOR', 'DEPUTY_CONVENOR', 'CORE', 'ASSOCIATE_CORE'))
                   OR (:filter = 'BATCH_REP' AND u.club_role = 'BATCH_REPRESENTATIVE')
                   OR (:filter = 'STUDENTS' AND (u.club_role = 'STUDENT' OR u.club_role IS NULL)))
                  AND (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
            ),
            user_scores AS (
                SELECT fu.id AS user_id,
                       fu.name,
                       fu.codeforces_handle,
                       fu.leetcode_handle,
                       fu.avatar_url,
                       fu.club_role,
                       ms.overall_score AS metric_value
                FROM filtered_users fu
                JOIN member_scores ms ON fu.id = ms.user_id
            ),
            ranked AS (
                SELECT user_id, name, codeforces_handle, leetcode_handle, avatar_url, club_role,
                       metric_value,
                       CAST(NULL AS INTEGER) AS basis_val,
                       RANK() OVER (ORDER BY metric_value DESC) AS placement
                FROM user_scores
            )
            """;
    }

    // ──────────────── Member Single Stats ────────────────

    public int countCodeforcesSolves(Long userId, Instant startUtc, Instant endUtc) {
        String sql = """
            SELECT COUNT(DISTINCT s.problem_id)
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            WHERE s.user_id = :userId
              AND p.contest_id < 100000
              AND s.first_ac_at >= :startUtc
              AND s.first_ac_at < :endUtc
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startUtc", Timestamp.from(startUtc))
                .addValue("endUtc", Timestamp.from(endUtc));
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    public int countContests(Long userId, Platform platform, Instant startUtc, Instant endUtc) {
        String sql = """
            SELECT COUNT(*)
            FROM contest_participations
            WHERE user_id = :userId
              AND platform = :platform
              AND started_at >= :startUtc
              AND started_at < :endUtc
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("platform", platform.name())
                .addValue("startUtc", Timestamp.from(startUtc))
                .addValue("endUtc", Timestamp.from(endUtc));
        Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null ? count : 0;
    }

    public RatedSolvesResult getCodeforcesAvgRating(Long userId, Instant startUtc, Instant endUtc) {
        String sql = """
            SELECT COUNT(p.rating) AS rated_count, AVG(p.rating) AS avg_rating
            FROM cf_solves s
            JOIN cf_problems p ON s.problem_id = p.id
            WHERE s.user_id = :userId
              AND p.contest_id < 100000
              AND p.rating IS NOT NULL
              AND s.first_ac_at >= :startUtc
              AND s.first_ac_at < :endUtc
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("startUtc", Timestamp.from(startUtc))
                .addValue("endUtc", Timestamp.from(endUtc));
        return jdbcTemplate.query(sql, params, rs -> {
            if (rs.next()) {
                int count = rs.getInt("rated_count");
                Double avg = rs.getObject("avg_rating") != null ? rs.getDouble("avg_rating") : null;
                return new RatedSolvesResult(avg, count);
            }
            return new RatedSolvesResult(null, 0);
        });
    }

    public BaselineResult getLeetCodeBaseline(Long userId, LocalDate onOrBeforeDate) {
        String sql = """
            SELECT total_solved, easy_solved, medium_solved, hard_solved
            FROM platform_daily_totals
            WHERE user_id = :userId
              AND platform = 'LEETCODE'
              AND captured_on <= :capturedOn
            ORDER BY captured_on DESC
            LIMIT 1
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("capturedOn", Date.valueOf(onOrBeforeDate));
        return jdbcTemplate.query(sql, params, rs -> {
            if (rs.next()) {
                return new BaselineResult(
                        rs.getInt("total_solved"),
                        (Integer) rs.getObject("easy_solved"),
                        (Integer) rs.getObject("medium_solved"),
                        (Integer) rs.getObject("hard_solved")
                );
            }
            return null;
        });
    }

    public LocalDate getLeetCodeEarliestCapturedOn(Long userId) {
        String sql = "SELECT MIN(captured_on) FROM platform_daily_totals WHERE user_id = :userId AND platform = 'LEETCODE'";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Date d = jdbcTemplate.queryForObject(sql, params, Date.class);
        return d != null ? d.toLocalDate() : null;
    }

    public LocalDate getLeetCodeOverallEarliestCapturedOn() {
        String sql = "SELECT MIN(captured_on) FROM platform_daily_totals WHERE platform = 'LEETCODE'";
        Date d = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Date.class);
        return d != null ? d.toLocalDate() : null;
    }

    public List<RatingPointDto> getRatingHistory(Long userId, Platform platform) {
        String sql = """
            SELECT started_at, new_rating, contest_name, external_contest_id, contest_rank
            FROM contest_participations
            WHERE user_id = :userId
              AND platform = :platform
              AND rated = true
              AND new_rating IS NOT NULL
            ORDER BY started_at ASC
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("platform", platform.name());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("started_at");
            LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : null;
            return RatingPointDto.builder()
                    .date(ldt)
                    .rating(rs.getInt("new_rating"))
                    .contestName(rs.getString("contest_name"))
                    .contestId(rs.getString("external_contest_id"))
                    .rank((Integer) rs.getObject("contest_rank"))
                    .build();
        });
    }

    public List<HeadToHeadContestDto> getSharedCodeforcesContests(Long aId, Long bId) {
        String sql = """
            SELECT a.external_contest_id,
                   a.contest_name,
                   a.started_at,
                   a.contest_rank AS rank_a,
                   b.contest_rank AS rank_b
            FROM contest_participations a
            JOIN contest_participations b ON a.external_contest_id = b.external_contest_id
                                          AND a.platform = b.platform
            WHERE a.user_id = :aId
              AND b.user_id = :bId
              AND a.platform = 'CODEFORCES'
              AND a.rated = true
              AND b.rated = true
              AND a.contest_rank IS NOT NULL
              AND b.contest_rank IS NOT NULL
            ORDER BY a.started_at DESC
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("aId", aId)
                .addValue("bId", bId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            Timestamp ts = rs.getTimestamp("started_at");
            LocalDateTime ldt = ts != null ? ts.toLocalDateTime() : null;
            return HeadToHeadContestDto.builder()
                    .externalContestId(rs.getString("external_contest_id"))
                    .contestName(rs.getString("contest_name"))
                    .startedAt(ldt)
                    .rankA((Integer) rs.getObject("rank_a"))
                    .rankB((Integer) rs.getObject("rank_b"))
                    .build();
        });
    }

    public LocalDateTime getMemberScoreComputedAt() {
        String sql = "SELECT MAX(computed_at) FROM member_scores";
        Timestamp ts = jdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Timestamp.class);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public MemberScoreDto getMemberScore(Long userId) {
        String sql = """
            SELECT overall_score, rating_score, solved_score, contest_score, computed_at
            FROM member_scores
            WHERE user_id = :userId
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        return jdbcTemplate.query(sql, params, rs -> {
            if (rs.next()) {
                Timestamp ts = rs.getTimestamp("computed_at");
                return MemberScoreDto.builder()
                        .overallScore(rs.getInt("overall_score"))
                        .ratingScore(rs.getInt("rating_score"))
                        .solvedScore(rs.getInt("solved_score"))
                        .contestScore(rs.getInt("contest_score"))
                        .computedAt(ts != null ? ts.toLocalDateTime() : null)
                        .build();
            }
            return null;
        });
    }
}
