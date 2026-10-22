package com.cpclub.backend.score.service;

import com.cpclub.backend.score.entity.MemberScore;
import com.cpclub.backend.score.repository.MemberScoreRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for computing the Overall Aggregated Score daily at 01:00 IST (Section 1.2, 6.5).
 * Replaces member_scores atomically within one transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MemberScoreService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MemberScoreRepository memberScoreRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Value("${cpclub.auth.require-email-verification:false}")
    private boolean requireEmailVerification;

    @Scheduled(cron = "0 0 1 * * *", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    @Transactional
    public void computeDailyScores() {
        log.info("Starting scheduled daily overall score computation at 01:00 IST");
        computeScores();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member_scores", new MapSqlParameterSource(), Long.class);
            if (count == null || count == 0) {
                log.info("Member scores table is empty on startup, computing initial scores");
                computeScores();
            } else {
                log.info("Member scores table already has {} records on startup", count);
            }
        } catch (Exception e) {
            log.warn("Could not check/initialize member scores on startup: {}", e.getMessage());
        }
    }
    // Manual syncs recompute scores once the job has finished -- see
    // SyncJobDispatcher. This used to listen for the trigger event itself, which
    // ran alongside the sync rather than after it and scored the old data.

    @Transactional
    public int computeScores() {
        Instant nowUtc = clock.instant();
        LocalDateTime computedAt = LocalDateTime.ofInstant(nowUtc, ZoneId.of("UTC"));
        Instant oneYearAgoUtc = nowUtc.minus(365, ChronoUnit.DAYS);

        String sql = """
            WITH verified_users AS (
                SELECT u.id, u.rating, u.leetcode_rating, COALESCE(u.leetcode_total_solved, 0) AS lc_solved
                FROM users u
                WHERE (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
            ),
            cf_solves_count AS (
                SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS cf_solved
                FROM cf_solves s
                JOIN cf_problems p ON s.problem_id = p.id
                WHERE (p.contest_id IS NULL OR p.contest_id < 100000)
                GROUP BY s.user_id
            ),
            contests_year AS (
                SELECT cp.user_id, COUNT(*) AS contest_count
                FROM contest_participations cp
                WHERE cp.started_at >= :oneYearAgoUtc
                GROUP BY cp.user_id
            ),
            user_metrics AS (
                SELECT vu.id AS user_id,
                       vu.rating AS cf_rating,
                       vu.leetcode_rating AS lc_rating,
                       (COALESCE(cs.cf_solved, 0) + vu.lc_solved) AS total_solved,
                       COALESCE(cy.contest_count, 0) AS total_contests
                FROM verified_users vu
                LEFT JOIN cf_solves_count cs ON vu.id = cs.user_id
                LEFT JOIN contests_year cy ON vu.id = cy.user_id
            ),
            active_users AS (
                SELECT * FROM user_metrics
                WHERE (cf_rating IS NOT NULL AND cf_rating > 0)
                   OR (lc_rating IS NOT NULL AND lc_rating > 0)
                   OR total_solved > 0
                   OR total_contests > 0
            ),
            cf_rated AS (
                SELECT user_id,
                       CASE WHEN COUNT(*) OVER () = 1 THEN 1.0
                            ELSE PERCENT_RANK() OVER (ORDER BY cf_rating ASC)
                       END AS cf_pct
                FROM active_users
                WHERE cf_rating IS NOT NULL AND cf_rating > 0
            ),
            lc_rated AS (
                SELECT user_id,
                       CASE WHEN COUNT(*) OVER () = 1 THEN 1.0
                            ELSE PERCENT_RANK() OVER (ORDER BY lc_rating ASC)
                       END AS lc_pct
                FROM active_users
                WHERE lc_rating IS NOT NULL AND lc_rating > 0
            ),
            solved_pcts AS (
                SELECT user_id,
                       CASE WHEN COUNT(*) OVER () = 1 THEN 1.0
                            ELSE PERCENT_RANK() OVER (ORDER BY total_solved ASC)
                       END AS solved_pct
                FROM active_users
            ),
            contest_pcts AS (
                SELECT user_id,
                       CASE WHEN COUNT(*) OVER () = 1 THEN 1.0
                            ELSE PERCENT_RANK() OVER (ORDER BY total_contests ASC)
                       END AS contest_pct
                FROM active_users
            ),
            computed_components AS (
                SELECT au.user_id,
                       ROUND(COALESCE(
                           CASE 
                               WHEN cfr.cf_pct IS NOT NULL AND lcr.lc_pct IS NOT NULL THEN GREATEST(cfr.cf_pct, lcr.lc_pct)
                               WHEN cfr.cf_pct IS NOT NULL THEN cfr.cf_pct
                               WHEN lcr.lc_pct IS NOT NULL THEN lcr.lc_pct
                               ELSE 0.0
                           END, 0.0) * 1000) AS rating_score,
                       ROUND(COALESCE(sp.solved_pct, 0.0) * 1000) AS solved_score,
                       ROUND(COALESCE(cp.contest_pct, 0.0) * 1000) AS contest_score
                FROM active_users au
                LEFT JOIN cf_rated cfr ON au.user_id = cfr.user_id
                LEFT JOIN lc_rated lcr ON au.user_id = lcr.user_id
                LEFT JOIN solved_pcts sp ON au.user_id = sp.user_id
                LEFT JOIN contest_pcts cp ON au.user_id = cp.user_id
            )
            SELECT user_id,
                   CAST(rating_score AS INTEGER) AS rating_score,
                   CAST(solved_score AS INTEGER) AS solved_score,
                   CAST(contest_score AS INTEGER) AS contest_score,
                   CAST(ROUND(0.5 * rating_score + 0.3 * solved_score + 0.2 * contest_score) AS INTEGER) AS overall_score
            FROM computed_components
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("requireEmailVerification", requireEmailVerification)
                .addValue("oneYearAgoUtc", Timestamp.from(oneYearAgoUtc));

        record ComputedRow(Long userId, int ratingScore, int solvedScore, int contestScore, int overallScore) {}

        List<ComputedRow> rows = jdbcTemplate.query(sql, params, (rs, rowNum) -> new ComputedRow(
                rs.getLong("user_id"),
                rs.getInt("rating_score"),
                rs.getInt("solved_score"),
                rs.getInt("contest_score"),
                rs.getInt("overall_score")
        ));

        jdbcTemplate.update("DELETE FROM member_scores", new MapSqlParameterSource());

        if (!rows.isEmpty()) {
            String insertSql = """
                INSERT INTO member_scores (user_id, overall_score, rating_score, solved_score, contest_score, computed_at)
                VALUES (:userId, :overallScore, :ratingScore, :solvedScore, :contestScore, :computedAt)
                """;
            List<MapSqlParameterSource> batchParams = rows.stream().map(r -> new MapSqlParameterSource()
                    .addValue("userId", r.userId())
                    .addValue("overallScore", r.overallScore())
                    .addValue("ratingScore", r.ratingScore())
                    .addValue("solvedScore", r.solvedScore())
                    .addValue("contestScore", r.contestScore())
                    .addValue("computedAt", Timestamp.valueOf(computedAt))
            ).toList();

            jdbcTemplate.batchUpdate(insertSql, batchParams.toArray(new MapSqlParameterSource[0]));
        }

        log.info("Completed overall score computation for {} members", rows.size());
        return rows.size();
    }
}
