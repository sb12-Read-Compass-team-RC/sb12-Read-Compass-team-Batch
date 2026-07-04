package com.rc.readcompassbatch.controller;

import com.rc.readcompassbatch.scheduler.BatchJobLauncher;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 모니터링/수동 실행용 REST API.
 *  - GET  /api/batch/status      : 최근 잡 실행 이력 + 랭킹 스냅샷 현황
 *  - POST /api/batch/run?job=... : 잡을 즉시 1회 실행 (rankingJob | maintenanceJob)
 *
 * 실행 이력은 Spring Batch 메타데이터 테이블(BATCH_JOB_EXECUTION)을 직접 조회한다.
 * 랭킹 현황은 각 랭킹 테이블의 최신 calculated_at 스냅샷 기준 건수를 센다.
 */
@Slf4j
@RestController
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class BatchMonitorController {

    private static final List<String> ALLOWED_JOBS = List.of("rankingJob", "maintenanceJob", "countSyncJob");

    private final BatchJobLauncher launcher;
    private final JdbcTemplate jdbc;

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("serverTime", OffsetDateTime.now().toString());
        result.put("recentExecutions", recentExecutions());

        Map<String, Object> rankings = new LinkedHashMap<>();
        rankings.put("book", snapshot("tb_book_rankings"));
        rankings.put("review", snapshot("tb_review_rankings"));
        rankings.put("user", snapshot("tb_user_rankings"));
        result.put("rankings", rankings);

        // countSyncJob 화면 표시용
        result.put("countSync", countSyncStatus());

        return result;
    }

    @PostMapping("/run")
    public Map<String, Object> run(@RequestParam String job) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (!ALLOWED_JOBS.contains(job)) {
            result.put("accepted", false);
            result.put("message", "알 수 없는 잡: " + job);
            return result;
        }

        // 비동기로 실행해 HTTP 응답은 즉시 반환하고, 화면 폴링으로 진행 상태를 확인한다.
        CompletableFuture.runAsync(() -> launcher.runByName(job));

        result.put("accepted", true);
        result.put("job", job);
        result.put("message", job + " 실행 요청됨");
        return result;
    }

    // ---------------------------------------------------------------
    // 최근 실행 이력 (BATCH_JOB_EXECUTION)
    // ---------------------------------------------------------------
    private List<Map<String, Object>> recentExecutions() {
        String sql = """
            SELECT i.job_name AS job_name,
                   e.status    AS status,
                   e.exit_code AS exit_code,
                   e.start_time AS start_time,
                   e.end_time   AS end_time
            FROM batch_job_execution e
            JOIN batch_job_instance i
                ON e.job_instance_id = i.job_instance_id
            ORDER BY e.job_execution_id DESC
            LIMIT 10
            """;

        List<Map<String, Object>> out = new ArrayList<>();

        try {
            for (Map<String, Object> r : jdbc.queryForList(sql)) {
                Object st = r.get("start_time");
                Object en = r.get("end_time");

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("jobName", r.get("job_name"));
                m.put("status", r.get("status"));
                m.put("exitCode", r.get("exit_code"));
                m.put("startTime", st == null ? null : st.toString());
                m.put("endTime", en == null ? null : en.toString());
                m.put("durationMs", durationMs(st, en));

                out.add(m);
            }
        } catch (Exception e) {
            log.warn("recentExecutions 조회 실패: {}", e.getMessage());
        }
        return out;
    }

    // ---------------------------------------------------------------
    // 랭킹 스냅샷 현황 (최신 calculated_at 기준 건수 + 기간별 분포)
    // table 값은 내부 고정 상수만 전달되므로 SQL 주입 위험 없음.
    // ---------------------------------------------------------------
    private Map<String, Object> snapshot(String table) {
        Map<String, Object> m = new LinkedHashMap<>();

        try {
            Timestamp latest = jdbc.queryForObject(
                "SELECT MAX(calculated_at) FROM " + table,
                    Timestamp.class
            );

            if (latest == null) {
                m.put("latestCalculatedAt", null);
                m.put("total", 0);
                m.put("byPeriod", new LinkedHashMap<>());
                return m;
            }

            m.put("latestCalculatedAt", latest.toInstant().toString());

            Integer total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE calculated_at = ?",
                    Integer.class,
                    latest
            );
            m.put("total", total == null ? 0 : total);

            Map<String, Object> byPeriod = new LinkedHashMap<>();
            for (Map<String, Object> r : jdbc.queryForList(
                "SELECT period_type, COUNT(*) AS cnt FROM " + table
                    + " WHERE calculated_at = ? GROUP BY period_type", latest)) {
                byPeriod.put(String.valueOf(r.get("period_type")), r.get("cnt"));
            }
            m.put("byPeriod", byPeriod);
        } catch (Exception e) {
            log.warn("snapshot 조회 실패 ({}): {}", table, e.getMessage());
            m.put("error", e.getMessage());
        }
        return m;
    }

    /**
     * countSyncJob 실행 전/후에 현재 누적 카운트 컬럼이 실제 row 수와 다른 대상이 몇 개인지 조회한다.
     */
    private Map<String, Object> countSyncStatus() {
        Map<String, Object> m = new LinkedHashMap<>();

        try {
            int bookReviewCountMismatch = countFor("""
                    SELECT COUNT(*)
                      FROM tb_books b
                      LEFT JOIN (
                            SELECT book_id,
                                   COUNT(*)::integer AS actual_count
                              FROM tb_reviews
                             WHERE is_deleted = false
                             GROUP BY book_id
                      ) r ON r.book_id = b.id
                     WHERE b.review_cnt IS DISTINCT FROM COALESCE(r.actual_count, 0)
                    """);

            int reviewLikeCountMismatch = countFor("""
                    SELECT COUNT(*)
                      FROM tb_reviews r
                      LEFT JOIN (
                            SELECT review_id,
                                   COUNT(*)::integer AS actual_count
                              FROM tb_review_likes
                             GROUP BY review_id
                      ) l ON l.review_id = r.id
                     WHERE r.like_cnt IS DISTINCT FROM COALESCE(l.actual_count, 0)
                    """);

            int reviewCommentCountMismatch = countFor("""
                    SELECT COUNT(*)
                      FROM tb_reviews r
                      LEFT JOIN (
                            SELECT review_id,
                                   COUNT(*)::integer AS actual_count
                              FROM tb_comments
                             WHERE is_deleted = false
                             GROUP BY review_id
                      ) c ON c.review_id = r.id
                     WHERE r.comment_cnt IS DISTINCT FROM COALESCE(c.actual_count, 0)
                    """);

            int totalMismatch =
                    bookReviewCountMismatch
                            + reviewLikeCountMismatch
                            + reviewCommentCountMismatch;

            m.put("bookReviewCountMismatch", bookReviewCountMismatch);
            m.put("reviewLikeCountMismatch", reviewLikeCountMismatch);
            m.put("reviewCommentCountMismatch", reviewCommentCountMismatch);
            m.put("totalMismatch", totalMismatch);
            m.put("ok", totalMismatch == 0);
        } catch (Exception e) {
            log.warn("countSyncStatus 조회 실패: {}", e.getMessage());
            m.put("error", e.getMessage());
            m.put("ok", false);
        }

        return m;
    }

    private int countFor(String sql) {
        Integer count = jdbc.queryForObject(sql, Integer.class);
        return count == null ? 0 : count;
    }

    private Long durationMs(Object start, Object end) {
        if (start instanceof Timestamp s && end instanceof Timestamp en) {
            return Duration.between(s.toInstant(), en.toInstant()).toMillis();
        }
        return null;
    }
}
