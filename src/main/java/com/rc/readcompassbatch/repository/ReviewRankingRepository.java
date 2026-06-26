package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.ReviewRanking;
import com.rc.readcompassbatch.repository.projection.ReviewScoreAggregate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRankingRepository extends JpaRepository<ReviewRanking, UUID> {

    /**
     * 기간 내 리뷰별 (좋아요 수, 댓글 수) 집계.
     * 인기 리뷰/파워 유저 점수 산출에는 논리 삭제된 댓글/리뷰도 포함하므로 is_deleted 필터를 두지 않는다.
     * 카티전 곱을 피하기 위해 COUNT(DISTINCT ...) FILTER 를 사용한다.
     *
     * <p>컬럼 별칭은 {@link ReviewScoreAggregate} 의 getter 명과 매핑되도록
     * 큰따옴표 카멜케이스로 고정한다(H2 PostgreSQL 모드 / PostgreSQL 모두 대소문자 보존).
     */
    @Query(value = """
        SELECT r.id AS "reviewId",
               COUNT(DISTINCT l.id) FILTER (WHERE l.created_at >= :from) AS "likeCount",
               COUNT(DISTINCT c.id) FILTER (WHERE c.created_at >= :from) AS "commentCount"
        FROM tb_reviews r
        LEFT JOIN tb_review_likes l ON l.review_id = r.id
        LEFT JOIN tb_comments c     ON c.review_id = r.id
        GROUP BY r.id
        """, nativeQuery = true)
    List<ReviewScoreAggregate> aggregate(@Param("from") Instant from);

    /** 리뷰 작성자 ID 조회 (TOP 10 알림 생성용). */
    @Query(value = "SELECT user_id FROM tb_reviews WHERE id = :reviewId", nativeQuery = true)
    UUID findAuthorId(@Param("reviewId") UUID reviewId);
}
