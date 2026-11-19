package com.cpclub.backend.cpgroup;

import com.cpclub.backend.cpgroup.dto.CpGroupMemberDetailDto;
import com.cpclub.backend.cpgroup.dto.CpGroupMemberRowDto;
import com.cpclub.backend.cpgroup.dto.CpGroupRequests;
import com.cpclub.backend.cpgroup.service.CpGroupService;
import com.cpclub.backend.support.PostgresIntegrationTest;
import com.cpclub.backend.user.entity.User;
import com.cpclub.backend.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The group's queries on real PostgreSQL with the V32 schema, outside a
 * transaction as in production: the grouped counts, the snapshot reads and the
 * entity's mapping (ddl-auto=validate) are what is under test.
 */
class CpGroupPostgresTest extends PostgresIntegrationTest {

    @Autowired private CpGroupService service;
    @Autowired private UserRepository userRepository;
    @Autowired private NamedParameterJdbcTemplate jdbc;

    private User student;
    private User admin;

    @AfterEach
    void tearDown() {
        MapSqlParameterSource ids = new MapSqlParameterSource("ids", List.of(student.getId(), admin.getId()));
        jdbc.update("DELETE FROM cp_group_members WHERE user_id IN (:ids)", ids);
        jdbc.update("DELETE FROM cf_solves WHERE user_id IN (:ids)", ids);
        jdbc.update("DELETE FROM cf_problems WHERE problem_key = '88001/A'", new MapSqlParameterSource());
        jdbc.update("DELETE FROM contest_participations WHERE user_id IN (:ids)", ids);
        jdbc.update("DELETE FROM platform_daily_totals WHERE user_id IN (:ids)", ids);
        userRepository.deleteAllById(List.of(student.getId(), admin.getId()));
    }

    @Test
    @DisplayName("List and detail on PostgreSQL")
    void listAndDetail() {
        admin = userRepository.save(User.builder().name("PG Admin").email("pg-cp-admin@dau.ac.in").build());
        student = userRepository.save(User.builder().name("PG Student").email("pg-cp-student@dau.ac.in")
                .codeforcesHandle("pg_cp_student").leetcodeHandle("pg_cp_lc").leetcodeTotalSolved(60).build());
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));

        jdbc.update("""
                INSERT INTO cf_problems (problem_key, contest_id, problem_index, name, rating, solved_count)
                VALUES ('88001/A', 88001, 'A', 'PG Problem', 1300, 5)
                """, new MapSqlParameterSource());
        jdbc.update("""
                INSERT INTO cf_solves (user_id, problem_id, first_ac_at, first_ac_submission_id, participant_type)
                SELECT :userId, id, :at, 1, 'PRACTICE' FROM cf_problems WHERE problem_key = '88001/A'
                """, new MapSqlParameterSource("userId", student.getId()).addValue("at", now.minusDays(2)));
        for (int[] snapshot : new int[][] {{10, 50}, {1, 58}, {0, 60}}) {
            jdbc.update("""
                    INSERT INTO platform_daily_totals (user_id, platform, captured_on, total_solved)
                    VALUES (:userId, 'LEETCODE', :on, :total)
                    """, new MapSqlParameterSource("userId", student.getId())
                    .addValue("on", today.minusDays(snapshot[0])).addValue("total", snapshot[1]));
        }
        jdbc.update("""
                INSERT INTO contest_participations (user_id, platform, external_contest_id, contest_name, started_at,
                                                    rated, contest_rank, old_rating, new_rating)
                VALUES (:userId, 'CODEFORCES', '880001', 'PG Round', :at, TRUE, 100, 1200, 1260)
                """, new MapSqlParameterSource("userId", student.getId()).addValue("at", now.minusDays(3)));

        service.addMembers(new CpGroupRequests.AddMembers(List.of(student.getId()), 2029), admin.getId());
        service.updateMember(student.getId(), new CpGroupRequests.UpdateMember(null, "Strong start"), admin.getId());

        CpGroupMemberRowDto row = service.listMembers().stream()
                .filter(r -> r.userId().equals(student.getId())).findFirst().orElseThrow();
        assertEquals(1, row.cfSolved7d());
        assertEquals(1, row.cfSolvedAll());
        assertEquals(10, row.lcSolved7d());
        assertEquals(60, row.lcSolvedAll());
        assertEquals(1, row.contests30d());
        assertEquals(60, row.cfRatingChange30d());
        assertEquals(today, row.lastActiveOn());

        CpGroupMemberDetailDto detail = service.memberDetail(student.getId());
        assertEquals(30, detail.daily().size());
        assertEquals(2, detail.daily().get(29).leetcode());
        assertEquals("Strong start", detail.notes());
        assertEquals("PG Admin", detail.addedBy());
        assertEquals(1, detail.recentSolves().size());
    }
}
