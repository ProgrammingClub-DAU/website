package com.cpclub.backend.cpgroup.service;

import com.cpclub.backend.common.exception.ResourceNotFoundException;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberDetailDto;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberDetailDto.ContestEntry;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberRowDto;
import com.cpclub.backend.cpgroup.dto.CpGroupRequests;
import com.cpclub.backend.cpgroup.entity.CpGroupMember;
import com.cpclub.backend.cpgroup.repository.CpGroupMemberRepository;
import com.cpclub.backend.stats.service.MemberActivityService;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * The CP Practice Group: who is in it, and how each student is doing.
 *
 * <p>Every read here is admin-only (see the controllers) except
 * {@link #myMembership}, which tells a student whether they are a member.</p>
 *
 * <p>Activity follows the leaderboard's definitions so the numbers agree across
 * the site: Codeforces solves are distinct problems from regular contests (no gym)
 * by first accepted submission; LeetCode solves are the rise in the member's total
 * since the daily snapshot at the start of the window. Windows are rolling 7 and
 * 30 days. All counts for the whole group come from a handful of grouped queries,
 * not one per student.</p>
 */
@Service
@RequiredArgsConstructor
public class CpGroupService {

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final int CONTEST_HISTORY = 300;
    /** LeetCode snapshots read back this far, so a 30-day baseline is found despite a missed day. */
    private static final int SNAPSHOT_LOOKBACK_DAYS = 45;

    private final CpGroupMemberRepository members;
    private final UserRepository userRepository;
    private final MemberActivityService memberActivity;
    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    /** The student and membership columns the rows are built from. */
    private record Base(Long userId, String name, String email, String avatarUrl,
                        String codeforcesHandle, String leetcodeHandle,
                        Integer batchYear, Integer profileBatchYear, LocalDateTime addedAt, Long addedBy,
                        String notes, LocalDateTime notesUpdatedAt, Long notesUpdatedBy,
                        Integer cfRating, Integer lcRating, Integer lcTotal) {}

    /** Every member with their activity, by group batch then name. */
    @Transactional(readOnly = true)
    public List<CpGroupMemberRowDto> listMembers() {
        return rows(loadBase(null));
    }

    @Transactional(readOnly = true)
    public CpGroupMemberDetailDto memberDetail(Long userId) {
        List<Base> base = loadBase(userId);
        if (base.isEmpty()) {
            throw new ResourceNotFoundException("Not in the CP Practice Group: " + userId);
        }
        Base member = base.get(0);

        List<ContestEntry> contests = jdbc.query("""
                SELECT platform, contest_name, started_at, rated, contest_rank, old_rating, new_rating
                FROM contest_participations
                WHERE user_id = :userId
                ORDER BY started_at DESC
                """ + " LIMIT " + CONTEST_HISTORY, new MapSqlParameterSource("userId", userId),
                (rs, i) -> new ContestEntry(
                        rs.getString("platform"),
                        rs.getString("contest_name"),
                        rs.getObject("started_at", LocalDateTime.class),
                        rs.getBoolean("rated"),
                        rs.getObject("contest_rank", Integer.class),
                        rs.getObject("old_rating", Integer.class),
                        rs.getObject("new_rating", Integer.class)));

        return new CpGroupMemberDetailDto(
                rows(base).get(0),
                member.notes(),
                member.notesUpdatedAt(),
                nameOf(member.notesUpdatedBy()),
                nameOf(member.addedBy()),
                memberActivity.dailySolves(userId),
                contests,
                memberActivity.recentSolves(userId));
    }

    /** Adds students to a batch; one already in the group moves to that batch. Unknown ids are ignored. */
    @Transactional
    public CpGroupRequests.AddResult addMembers(CpGroupRequests.AddMembers request, Long adminId) {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        int added = 0;
        int moved = 0;
        for (User user : userRepository.findAllById(new LinkedHashSet<>(request.userIds()))) {
            Optional<CpGroupMember> existing = members.findById(user.getId());
            if (existing.isEmpty()) {
                members.save(new CpGroupMember(user, request.batchYear(), now, adminId));
                added++;
            } else if (!existing.get().getBatchYear().equals(request.batchYear())) {
                existing.get().setBatchYear(request.batchYear());
                moved++;
            }
        }
        return new CpGroupRequests.AddResult(added, moved);
    }

    /** Changes a student's batch and/or notes; empty notes clear them. */
    @Transactional
    public void updateMember(Long userId, CpGroupRequests.UpdateMember request, Long adminId) {
        CpGroupMember member = members.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Not in the CP Practice Group: " + userId));
        if (request.batchYear() != null) {
            member.setBatchYear(request.batchYear());
        }
        if (request.notes() != null) {
            String notes = request.notes().strip();
            member.setNotes(notes.isEmpty() ? null : notes);
            member.setNotesUpdatedAt(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
            member.setNotesUpdatedBy(adminId);
        }
    }

    @Transactional
    public void removeMember(Long userId) {
        members.deleteById(userId);
    }

    /** Whether this student is in the group: all a student is shown of it. */
    @Transactional(readOnly = true)
    public CpGroupRequests.MyMembership myMembership(Long userId) {
        return members.findById(userId)
                .map(m -> new CpGroupRequests.MyMembership(true, m.getBatchYear(), m.getAddedAt()))
                .orElse(new CpGroupRequests.MyMembership(false, null, null));
    }

    private List<Base> loadBase(Long onlyUserId) {
        String sql = """
                SELECT g.user_id, g.batch_year, g.added_at, g.added_by, g.notes, g.notes_updated_at, g.notes_updated_by,
                       u.name, u.email, u.avatar_url, u.codeforces_handle, u.leetcode_handle,
                       u.batch_year AS profile_batch_year, u.rating, u.leetcode_rating, u.leetcode_total_solved
                FROM cp_group_members g JOIN users u ON u.id = g.user_id
                """ + (onlyUserId != null ? " WHERE g.user_id = :userId" : "")
                + " ORDER BY g.batch_year, u.name";
        return jdbc.query(sql, new MapSqlParameterSource("userId", onlyUserId), (rs, i) -> new Base(
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("avatar_url"),
                rs.getString("codeforces_handle"),
                rs.getString("leetcode_handle"),
                rs.getObject("batch_year", Integer.class),
                rs.getObject("profile_batch_year", Integer.class),
                rs.getObject("added_at", LocalDateTime.class),
                rs.getObject("added_by", Long.class),
                rs.getString("notes"),
                rs.getObject("notes_updated_at", LocalDateTime.class),
                rs.getObject("notes_updated_by", Long.class),
                rs.getObject("rating", Integer.class),
                rs.getObject("leetcode_rating", Integer.class),
                rs.getObject("leetcode_total_solved", Integer.class)));
    }

    private List<CpGroupMemberRowDto> rows(List<Base> base) {
        if (base.isEmpty()) return List.of();
        List<Long> ids = base.stream().map(Base::userId).toList();
        Instant now = clock.instant();
        LocalDateTime nowUtc = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LocalDateTime since30 = nowUtc.minusDays(30);
        LocalDate todayIst = LocalDate.ofInstant(now, IST);

        // Codeforces: both windows, the total and the latest solve, in one pass.
        Map<Long, int[]> cfCounts = new HashMap<>();
        Map<Long, LocalDate> lastActive = new HashMap<>();
        jdbc.query("""
                SELECT s.user_id,
                       SUM(CASE WHEN s.first_ac_at >= :since7 THEN 1 ELSE 0 END) AS d7,
                       SUM(CASE WHEN s.first_ac_at >= :since30 THEN 1 ELSE 0 END) AS d30,
                       COUNT(*) AS total,
                       MAX(s.first_ac_at) AS last_solve
                FROM cf_solves s JOIN cf_problems p ON p.id = s.problem_id
                WHERE s.user_id IN (:ids) AND (p.contest_id IS NULL OR p.contest_id < 100000)
                GROUP BY s.user_id
                """,
                new MapSqlParameterSource("ids", ids)
                        .addValue("since7", nowUtc.minusDays(7))
                        .addValue("since30", since30),
                rs -> {
                    long userId = rs.getLong("user_id");
                    cfCounts.put(userId, new int[] {rs.getInt("d7"), rs.getInt("d30"), rs.getInt("total")});
                    LocalDateTime lastSolve = rs.getObject("last_solve", LocalDateTime.class);
                    if (lastSolve != null) lastActive.merge(userId, istDate(lastSolve), CpGroupService::later);
                });

        // LeetCode: the change since each window's baseline, and the latest day the total rose.
        Map<Long, NavigableMap<LocalDate, Integer>> snapshots =
                leetcodeSnapshots(ids, todayIst.minusDays(SNAPSHOT_LOOKBACK_DAYS));
        snapshots.forEach((userId, byDay) -> {
            Integer previous = null;
            for (Map.Entry<LocalDate, Integer> day : byDay.entrySet()) {
                if (previous != null && day.getValue() > previous) {
                    lastActive.merge(userId, day.getKey(), CpGroupService::later);
                }
                previous = day.getValue();
            }
        });

        // Contests: the last 30 days' count and rating change per platform, and the latest contest.
        Map<Long, Integer> contests30 = new HashMap<>();
        Map<Long, Integer> cfChange = new HashMap<>();
        Map<Long, Integer> lcChange = new HashMap<>();
        jdbc.query("""
                SELECT user_id, platform, rated, old_rating, new_rating
                FROM contest_participations
                WHERE user_id IN (:ids) AND started_at >= :since30
                """,
                new MapSqlParameterSource("ids", ids).addValue("since30", since30),
                rs -> {
                    long userId = rs.getLong("user_id");
                    contests30.merge(userId, 1, Integer::sum);
                    Integer oldRating = rs.getObject("old_rating", Integer.class);
                    Integer newRating = rs.getObject("new_rating", Integer.class);
                    if (rs.getBoolean("rated") && oldRating != null && newRating != null) {
                        Map<Long, Integer> change = "LEETCODE".equals(rs.getString("platform")) ? lcChange : cfChange;
                        change.merge(userId, newRating - oldRating, Integer::sum);
                    }
                });
        jdbc.query("""
                SELECT user_id, MAX(started_at) AS last_contest
                FROM contest_participations WHERE user_id IN (:ids) GROUP BY user_id
                """,
                new MapSqlParameterSource("ids", ids),
                rs -> {
                    LocalDateTime lastContest = rs.getObject("last_contest", LocalDateTime.class);
                    if (lastContest != null) lastActive.merge(rs.getLong("user_id"), istDate(lastContest), CpGroupService::later);
                });

        List<CpGroupMemberRowDto> rows = new ArrayList<>();
        for (Base b : base) {
            int[] cf = cfCounts.getOrDefault(b.userId(), new int[3]);
            NavigableMap<LocalDate, Integer> lc = snapshots.getOrDefault(b.userId(), new TreeMap<>());
            rows.add(new CpGroupMemberRowDto(
                    b.userId(), b.name(), b.email(), b.avatarUrl(), b.codeforcesHandle(), b.leetcodeHandle(),
                    b.batchYear(), b.profileBatchYear(), b.addedAt(),
                    cf[0], cf[1], cf[2],
                    leetcodeSince(b.lcTotal(), lc, todayIst.minusDays(7)),
                    leetcodeSince(b.lcTotal(), lc, todayIst.minusDays(30)),
                    b.lcTotal(),
                    b.cfRating(), cfChange.get(b.userId()),
                    b.lcRating(), lcChange.get(b.userId()),
                    contests30.getOrDefault(b.userId(), 0),
                    lastActive.get(b.userId()),
                    b.notes() != null && !b.notes().isBlank()));
        }
        return rows;
    }

    /** Daily LeetCode totals per member from {@code from} on, by date. */
    private Map<Long, NavigableMap<LocalDate, Integer>> leetcodeSnapshots(List<Long> ids, LocalDate from) {
        Map<Long, NavigableMap<LocalDate, Integer>> byUser = new HashMap<>();
        jdbc.query("""
                SELECT user_id, captured_on, total_solved
                FROM platform_daily_totals
                WHERE platform = 'LEETCODE' AND user_id IN (:ids) AND captured_on >= :from
                """,
                new MapSqlParameterSource("ids", ids).addValue("from", from),
                rs -> {
                    byUser.computeIfAbsent(rs.getLong("user_id"), k -> new TreeMap<>())
                            .put(rs.getObject("captured_on", LocalDate.class), rs.getInt("total_solved"));
                });
        return byUser;
    }

    /**
     * Solved since {@code start}: the current total less the latest snapshot on or
     * before it. Null with no total or no snapshot that far back.
     */
    private static Integer leetcodeSince(Integer total, NavigableMap<LocalDate, Integer> snapshots, LocalDate start) {
        if (total == null) return null;
        Map.Entry<LocalDate, Integer> baseline = snapshots.floorEntry(start);
        return baseline == null ? null : Math.max(0, total - baseline.getValue());
    }

    private String nameOf(Long userId) {
        return userId == null ? null : userRepository.findById(userId).map(User::getName).orElse(null);
    }

    private static LocalDate istDate(LocalDateTime utc) {
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(IST).toLocalDate();
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }
}
