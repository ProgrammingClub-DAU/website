package com.cpclub.backend.score.service;

import com.cpclub.backend.common.exception.BadRequestException;
import com.cpclub.backend.common.model.Platform;
import com.cpclub.backend.score.dto.LatestWeeklyAwardsResponseDto;
import com.cpclub.backend.score.dto.WeeklyAwardDto;
import com.cpclub.backend.score.entity.AwardType;
import com.cpclub.backend.score.entity.WeeklyAward;
import com.cpclub.backend.score.repository.WeeklyAwardRepository;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/**
 * Service for computing and querying weekly awards (Section 1.5, 6.6).
 * Evaluates the closed week (Monday 00:00 to Monday 00:00 IST).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyAwardService {

    private final WeeklyAwardRepository weeklyAwardRepository;
    private final UserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Clock clock;

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    @Value("${cpclub.auth.require-email-verification:false}")
    private boolean requireEmailVerification;

    @Scheduled(cron = "0 30 0 * * MON", zone = "${cpclub.scheduling.zone:Asia/Kolkata}")
    @Transactional
    public void computeScheduledWeeklyAwards() {
        LocalDate todayIst = LocalDate.ofInstant(clock.instant(), IST_ZONE);
        // Scheduled runs on Monday at 00:30 IST; the closed week began 7 days ago
        LocalDate closedWeekStart = todayIst.minusDays(7);
        log.info("Starting scheduled weekly awards computation for closed week: {}", closedWeekStart);
        computeWeek(closedWeekStart, null);
    }

    @Transactional
    public List<WeeklyAwardDto> computeWeek(LocalDate weekStart, Long adminId) {
        LocalDate todayIst = LocalDate.ofInstant(clock.instant(), IST_ZONE);
        LocalDate currentWeekStart = todayIst.minusDays(todayIst.getDayOfWeek().getValue() - 1);

        if (!weekStart.isBefore(currentWeekStart)) {
            throw new BadRequestException("Cannot compute awards for the current or a future week; requested: " + weekStart);
        }

        User adminUser = null;
        if (adminId != null) {
            adminUser = userRepository.findById(adminId)
                    .orElseThrow(() -> new BadRequestException("Admin user not found with id: " + adminId));
            weeklyAwardRepository.deleteByWeekStart(weekStart);
            weeklyAwardRepository.flush();
        }

        LocalDate weekEnd = weekStart.plusDays(7);
        Instant startUtc = weekStart.atStartOfDay(IST_ZONE).toInstant();
        Instant endUtc = weekEnd.atStartOfDay(IST_ZONE).toInstant();
        LocalDateTime computedAt = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("UTC"));

        List<WeeklyAward> newAwards = new ArrayList<>();

        // 1. MOST_SOLVED
        if (adminId != null || !weeklyAwardRepository.existsByWeekStartAndAwardType(weekStart, AwardType.MOST_SOLVED)) {
            newAwards.addAll(computeMostSolved(weekStart, weekEnd, startUtc, endUtc, computedAt, adminUser));
        }

        // 2. BIGGEST_RATING_JUMP
        if (adminId != null || !weeklyAwardRepository.existsByWeekStartAndAwardType(weekStart, AwardType.BIGGEST_RATING_JUMP)) {
            newAwards.addAll(computeBiggestRatingJump(weekStart, startUtc, endUtc, computedAt, adminUser));
        }

        // 3. CODER_OF_THE_WEEK
        if (adminId != null || !weeklyAwardRepository.existsByWeekStartAndAwardType(weekStart, AwardType.CODER_OF_THE_WEEK)) {
            newAwards.addAll(computeCoderOfTheWeek(weekStart, weekEnd, startUtc, endUtc, computedAt, adminUser));
        }

        if (!newAwards.isEmpty()) {
            weeklyAwardRepository.saveAll(newAwards);
            weeklyAwardRepository.flush();
        }

        return getAwardsByWeek(weekStart);
    }

    private List<WeeklyAward> computeMostSolved(
            LocalDate weekStart, LocalDate weekEnd, Instant startUtc, Instant endUtc,
            LocalDateTime computedAt, User adminUser
    ) {
        String sql = """
            WITH verified_users AS (
                SELECT u.id
                FROM users u
                WHERE (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
            ),
            cf_solves_week AS (
                SELECT s.user_id, COUNT(DISTINCT s.problem_id) AS cf_solved
                FROM cf_solves s
                JOIN cf_problems p ON s.problem_id = p.id
                WHERE p.contest_id < 100000
                  AND s.first_ac_at >= :startUtc
                  AND s.first_ac_at < :endUtc
                GROUP BY s.user_id
            ),
            start_bl AS (
                SELECT user_id, total_solved FROM platform_daily_totals
                WHERE platform = 'LEETCODE' AND captured_on = :weekStartIst
            ),
            end_bl AS (
                SELECT user_id, total_solved FROM platform_daily_totals
                WHERE platform = 'LEETCODE' AND captured_on = :weekEndIst
            ),
            lc_solves_week AS (
                SELECT s.user_id, GREATEST(0, e.total_solved - s.total_solved) AS lc_solved
                FROM start_bl s
                JOIN end_bl e ON s.user_id = e.user_id
            ),
            totals AS (
                SELECT vu.id AS user_id,
                       (COALESCE(cf.cf_solved, 0) + COALESCE(lc.lc_solved, 0)) AS metric_val
                FROM verified_users vu
                LEFT JOIN cf_solves_week cf ON vu.id = cf.user_id
                LEFT JOIN lc_solves_week lc ON vu.id = lc.user_id
                WHERE (COALESCE(cf.cf_solved, 0) + COALESCE(lc.lc_solved, 0)) > 0
            )
            SELECT user_id, metric_val
            FROM totals
            WHERE metric_val = (SELECT MAX(metric_val) FROM totals)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("requireEmailVerification", requireEmailVerification)
                .addValue("startUtc", LocalDateTime.ofInstant(startUtc, ZoneOffset.UTC))
                .addValue("endUtc", LocalDateTime.ofInstant(endUtc, ZoneOffset.UTC))
                .addValue("weekStartIst", Date.valueOf(weekStart))
                .addValue("weekEndIst", Date.valueOf(weekEnd));

        List<WeeklyAward> awards = new ArrayList<>();
        jdbcTemplate.query(sql, params, rs -> {
            Long userId = rs.getLong("user_id");
            int val = rs.getInt("metric_val");
            awards.add(WeeklyAward.builder()
                    .weekStart(weekStart)
                    .awardType(AwardType.MOST_SOLVED)
                    .user(userRepository.getReferenceById(userId))
                    .metricValue(val)
                    .platform(null)
                    .computedAt(computedAt)
                    .computedBy(adminUser)
                    .build());
        });
        return awards;
    }

    private List<WeeklyAward> computeBiggestRatingJump(
            LocalDate weekStart, Instant startUtc, Instant endUtc,
            LocalDateTime computedAt, User adminUser
    ) {
        String sql = """
            WITH verified_users AS (
                SELECT u.id
                FROM users u
                WHERE (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
            ),
            platform_jumps AS (
                SELECT cp.user_id, cp.platform, SUM(cp.new_rating - cp.old_rating) AS net_jump
                FROM contest_participations cp
                JOIN verified_users vu ON cp.user_id = vu.id
                WHERE cp.rated = true
                  AND cp.old_rating IS NOT NULL
                  AND cp.new_rating IS NOT NULL
                  AND cp.started_at >= :startUtc
                  AND cp.started_at < :endUtc
                GROUP BY cp.user_id, cp.platform
                HAVING SUM(cp.new_rating - cp.old_rating) > 0
            ),
            user_best AS (
                SELECT pj.user_id, pj.platform, pj.net_jump,
                       ROW_NUMBER() OVER (PARTITION BY pj.user_id ORDER BY pj.net_jump DESC, pj.platform ASC) AS rn
                FROM platform_jumps pj
            ),
            ranked_users AS (
                SELECT user_id, platform, net_jump
                FROM user_best
                WHERE rn = 1
            )
            SELECT user_id, platform, net_jump
            FROM ranked_users
            WHERE net_jump = (SELECT MAX(net_jump) FROM ranked_users)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("requireEmailVerification", requireEmailVerification)
                .addValue("startUtc", LocalDateTime.ofInstant(startUtc, ZoneOffset.UTC))
                .addValue("endUtc", LocalDateTime.ofInstant(endUtc, ZoneOffset.UTC));

        List<WeeklyAward> awards = new ArrayList<>();
        jdbcTemplate.query(sql, params, rs -> {
            Long userId = rs.getLong("user_id");
            String platformStr = rs.getString("platform");
            int jump = rs.getInt("net_jump");
            awards.add(WeeklyAward.builder()
                    .weekStart(weekStart)
                    .awardType(AwardType.BIGGEST_RATING_JUMP)
                    .user(userRepository.getReferenceById(userId))
                    .metricValue(jump)
                    .platform(Platform.valueOf(platformStr))
                    .computedAt(computedAt)
                    .computedBy(adminUser)
                    .build());
        });
        return awards;
    }

    private List<WeeklyAward> computeCoderOfTheWeek(
            LocalDate weekStart, LocalDate weekEnd, Instant startUtc, Instant endUtc,
            LocalDateTime computedAt, User adminUser
    ) {
        String sql = """
            WITH verified_users AS (
                SELECT u.id
                FROM users u
                WHERE (:requireEmailVerification = false OR u.email_verified_at IS NOT NULL)
            ),
            cf_points AS (
                SELECT s.user_id,
                       SUM(CASE WHEN p.rating IS NOT NULL THEN GREATEST(8, p.rating / 100) ELSE 8 END) AS cf_pts
                FROM cf_solves s
                JOIN cf_problems p ON s.problem_id = p.id
                WHERE p.contest_id < 100000
                  AND s.first_ac_at >= :startUtc
                  AND s.first_ac_at < :endUtc
                GROUP BY s.user_id
            ),
            start_bl AS (
                SELECT user_id, easy_solved, medium_solved, hard_solved FROM platform_daily_totals
                WHERE platform = 'LEETCODE' AND captured_on = :weekStartIst
            ),
            end_bl AS (
                SELECT user_id, easy_solved, medium_solved, hard_solved FROM platform_daily_totals
                WHERE platform = 'LEETCODE' AND captured_on = :weekEndIst
            ),
            lc_points AS (
                SELECT s.user_id,
                       (GREATEST(0, COALESCE(e.easy_solved, 0) - COALESCE(s.easy_solved, 0)) * 5 +
                        GREATEST(0, COALESCE(e.medium_solved, 0) - COALESCE(s.medium_solved, 0)) * 12 +
                        GREATEST(0, COALESCE(e.hard_solved, 0) - COALESCE(s.hard_solved, 0)) * 25) AS lc_pts
                FROM start_bl s
                JOIN end_bl e ON s.user_id = e.user_id
            ),
            contest_points AS (
                SELECT cp.user_id, COUNT(*) * 20 AS contest_pts
                FROM contest_participations cp
                WHERE cp.started_at >= :startUtc
                  AND cp.started_at < :endUtc
                GROUP BY cp.user_id
            ),
            rating_gain AS (
                SELECT cp.user_id,
                       GREATEST(0, SUM(cp.new_rating - cp.old_rating)) / 5 AS rating_pts
                FROM contest_participations cp
                WHERE cp.rated = true
                  AND cp.old_rating IS NOT NULL
                  AND cp.new_rating IS NOT NULL
                  AND cp.started_at >= :startUtc
                  AND cp.started_at < :endUtc
                GROUP BY cp.user_id
                HAVING SUM(cp.new_rating - cp.old_rating) > 0
            ),
            total_points AS (
                SELECT vu.id AS user_id,
                       (COALESCE(cf.cf_pts, 0) +
                        COALESCE(lc.lc_pts, 0) +
                        COALESCE(ct.contest_pts, 0) +
                        COALESCE(rg.rating_pts, 0)) AS total_pts
                FROM verified_users vu
                LEFT JOIN cf_points cf ON vu.id = cf.user_id
                LEFT JOIN lc_points lc ON vu.id = lc.user_id
                LEFT JOIN contest_points ct ON vu.id = ct.user_id
                LEFT JOIN rating_gain rg ON vu.id = rg.user_id
                WHERE (COALESCE(cf.cf_pts, 0) +
                       COALESCE(lc.lc_pts, 0) +
                       COALESCE(ct.contest_pts, 0) +
                       COALESCE(rg.rating_pts, 0)) > 0
            )
            SELECT user_id, total_pts
            FROM total_points
            WHERE total_pts = (SELECT MAX(total_pts) FROM total_points)
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("requireEmailVerification", requireEmailVerification)
                .addValue("startUtc", LocalDateTime.ofInstant(startUtc, ZoneOffset.UTC))
                .addValue("endUtc", LocalDateTime.ofInstant(endUtc, ZoneOffset.UTC))
                .addValue("weekStartIst", Date.valueOf(weekStart))
                .addValue("weekEndIst", Date.valueOf(weekEnd));

        List<WeeklyAward> awards = new ArrayList<>();
        jdbcTemplate.query(sql, params, rs -> {
            Long userId = rs.getLong("user_id");
            int pts = rs.getInt("total_pts");
            awards.add(WeeklyAward.builder()
                    .weekStart(weekStart)
                    .awardType(AwardType.CODER_OF_THE_WEEK)
                    .user(userRepository.getReferenceById(userId))
                    .metricValue(pts)
                    .platform(null)
                    .computedAt(computedAt)
                    .computedBy(adminUser)
                    .build());
        });
        return awards;
    }

    public LatestWeeklyAwardsResponseDto getLatestWeeklyAwards() {
        LocalDate latestWeek = weeklyAwardRepository.findLatestWeekStart();
        if (latestWeek == null) {
            LocalDate todayIst = LocalDate.ofInstant(clock.instant(), IST_ZONE);
            LocalDate lastMonday = todayIst.minusDays(todayIst.getDayOfWeek().getValue() - 1).minusDays(7);
            return LatestWeeklyAwardsResponseDto.builder()
                    .weekStart(lastMonday)
                    .computed(false)
                    .awards(List.of())
                    .build();
        }

        List<WeeklyAwardDto> awards = getAwardsByWeek(latestWeek);
        return LatestWeeklyAwardsResponseDto.builder()
                .weekStart(latestWeek)
                .computed(true)
                .awards(awards)
                .build();
    }

    public List<WeeklyAwardDto> getAwardsByWeek(LocalDate weekStart) {
        List<WeeklyAward> entities = weeklyAwardRepository.findByWeekStartOrderByAwardTypeAscUserIdAsc(weekStart);
        return entities.stream().map(this::toDto).toList();
    }

    public List<WeeklyAwardDto> getUserAwards(Long userId) {
        List<WeeklyAward> entities = weeklyAwardRepository.findByUserIdOrderByWeekStartDescAwardTypeAsc(userId);
        return entities.stream().map(this::toDto).toList();
    }

    private WeeklyAwardDto toDto(WeeklyAward entity) {
        User u = entity.getUser();
        return WeeklyAwardDto.builder()
                .id(entity.getId())
                .weekStart(entity.getWeekStart())
                .awardType(entity.getAwardType())
                .userId(u != null ? u.getId() : null)
                .userName(u != null ? u.getName() : null)
                .codeforcesHandle(u != null ? u.getCodeforcesHandle() : null)
                .leetcodeHandle(u != null ? u.getLeetcodeHandle() : null)
                .avatarUrl(u != null ? u.getAvatarUrl() : null)
                .clubRole(u != null ? u.getClubRole() : null)
                .metricValue(entity.getMetricValue())
                .platform(entity.getPlatform())
                .computedAt(entity.getComputedAt())
                .build();
    }
}
