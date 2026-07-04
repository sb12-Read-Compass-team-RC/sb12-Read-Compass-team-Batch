package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.dto.CountMismatchDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CountSyncRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * tb_books.review_cnt = 논리 삭제되지 않은 리뷰 수
     */
    public int syncBookReviewCount() {
        return jdbcTemplate.update("""
                UPDATE tb_books b
                   SET review_cnt = COALESCE(rc.review_cnt, 0),
                       updated_at = CURRENT_TIMESTAMP
                  FROM (
                        SELECT b2.id AS book_id,
                               COUNT(r.id)::integer AS review_cnt
                          FROM tb_books b2
                          LEFT JOIN tb_reviews r
                            ON r.book_id = b2.id
                           AND r.is_deleted = false
                         GROUP BY b2.id
                       ) rc
                 WHERE b.id = rc.book_id
                   AND b.review_cnt IS DISTINCT FROM rc.review_cnt
                """);
    }

    /**
     * tb_books.rating = 논리 삭제되지 않은 리뷰 평점 평균
     */
    public int syncBookRating() {
        return jdbcTemplate.update("""
                UPDATE tb_books b
                   SET rating = COALESCE(rs.rating_avg, 0),
                       updated_at = CURRENT_TIMESTAMP
                  FROM (
                        SELECT b2.id AS book_id,
                               COALESCE(AVG(r.rating), 0)::double precision AS rating_avg
                          FROM tb_books b2
                          LEFT JOIN tb_reviews r
                            ON r.book_id = b2.id
                           AND r.is_deleted = false
                         GROUP BY b2.id
                       ) rs
                 WHERE b.id = rs.book_id
                   AND b.rating IS DISTINCT FROM rs.rating_avg
                """);
    }

    /**
     * tb_reviews.like_cnt = tb_review_likes row 수
     */
    public int syncReviewLikeCount() {
        return jdbcTemplate.update("""
                UPDATE tb_reviews r
                   SET like_cnt = COALESCE(lc.like_cnt, 0),
                       updated_at = CURRENT_TIMESTAMP
                  FROM (
                        SELECT r2.id AS review_id,
                               COUNT(l.id)::integer AS like_cnt
                          FROM tb_reviews r2
                          LEFT JOIN tb_review_likes l
                            ON l.review_id = r2.id
                         GROUP BY r2.id
                       ) lc
                 WHERE r.id = lc.review_id
                   AND r.like_cnt IS DISTINCT FROM lc.like_cnt
                """);
    }

    /**
     * tb_reviews.comment_cnt = 논리 삭제되지 않은 댓글 수
     */
    public int syncReviewCommentCount() {
        return jdbcTemplate.update("""
                UPDATE tb_reviews r
                   SET comment_cnt = COALESCE(cc.comment_cnt, 0),
                       updated_at = CURRENT_TIMESTAMP
                  FROM (
                        SELECT r2.id AS review_id,
                               COUNT(c.id)::integer AS comment_cnt
                          FROM tb_reviews r2
                          LEFT JOIN tb_comments c
                            ON c.review_id = r2.id
                           AND c.is_deleted = false
                         GROUP BY r2.id
                       ) cc
                 WHERE r.id = cc.review_id
                   AND r.comment_cnt IS DISTINCT FROM cc.comment_cnt
                """);
    }

    public List<CountMismatchDetail> findBookReviewCountMismatches() {
        return jdbcTemplate.query("""
            SELECT 'BOOK_REVIEW_COUNT' AS type,
                   b.id AS target_id,
                   b.review_cnt AS saved_count,
                   COALESCE(actual.actual_count, 0) AS actual_count
              FROM tb_books b
              LEFT JOIN (
                    SELECT book_id,
                           COUNT(*)::integer AS actual_count
                      FROM tb_reviews
                     WHERE is_deleted = false
                     GROUP BY book_id
              ) actual ON actual.book_id = b.id
             WHERE b.review_cnt IS DISTINCT FROM COALESCE(actual.actual_count, 0)
             ORDER BY b.id
             LIMIT 100
            """, (rs, rowNum) -> new CountMismatchDetail(
                rs.getString("type"),
                rs.getObject("target_id", UUID.class),
                rs.getInt("saved_count"),
                rs.getInt("actual_count")
        ));
    }

    public List<CountMismatchDetail> findReviewLikeCountMismatches() {
        return jdbcTemplate.query("""
            SELECT 'REVIEW_LIKE_COUNT' AS type,
                   r.id AS target_id,
                   r.like_cnt AS saved_count,
                   COALESCE(actual.actual_count, 0) AS actual_count
              FROM tb_reviews r
              LEFT JOIN (
                    SELECT review_id,
                           COUNT(*)::integer AS actual_count
                      FROM tb_review_likes
                     GROUP BY review_id
              ) actual ON actual.review_id = r.id
             WHERE r.like_cnt IS DISTINCT FROM COALESCE(actual.actual_count, 0)
             ORDER BY r.id
             LIMIT 100
            """, (rs, rowNum) -> new CountMismatchDetail(
                rs.getString("type"),
                rs.getObject("target_id", UUID.class),
                rs.getInt("saved_count"),
                rs.getInt("actual_count")
        ));
    }

    public List<CountMismatchDetail> findReviewCommentCountMismatches() {
        return jdbcTemplate.query("""
            SELECT 'REVIEW_COMMENT_COUNT' AS type,
                   r.id AS target_id,
                   r.comment_cnt AS saved_count,
                   COALESCE(actual.actual_count, 0) AS actual_count
              FROM tb_reviews r
              LEFT JOIN (
                    SELECT review_id,
                           COUNT(*)::integer AS actual_count
                      FROM tb_comments
                     WHERE is_deleted = false
                     GROUP BY review_id
              ) actual ON actual.review_id = r.id
             WHERE r.comment_cnt IS DISTINCT FROM COALESCE(actual.actual_count, 0)
             ORDER BY r.id
             LIMIT 100
            """, (rs, rowNum) -> new CountMismatchDetail(
                rs.getString("type"),
                rs.getObject("target_id", UUID.class),
                rs.getInt("saved_count"),
                rs.getInt("actual_count")
        ));
    }
}