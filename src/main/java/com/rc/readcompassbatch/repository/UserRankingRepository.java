package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.UserRanking;
import com.rc.readcompassbatch.repository.projection.UserScoreAggregate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRankingRepository extends JpaRepository<UserRanking, UUID> {

    /**
     * 기간 내 유저별 활동 점수 구성요소 집계.
     *  - reviewScore     : 작성 리뷰들의 인기 점수 합 = Σ(기간 좋아요*0.3 + 기간 댓글*0.7)
     *  - givenLikes      : 유저가 누른 좋아요 수
     *  - givenComments   : 유저가 작성한 댓글 수
     *
     * <p>컬럼 별칭은 {@link UserScoreAggregate} 의 getter 명과 매핑되도록
     * 큰따옴표 카멜케이스로 고정한다(H2 PostgreSQL 모드 / PostgreSQL 모두 대소문자 보존).
     */
    @Query(value = """
        WITH review_pop AS (
            SELECT r.user_id AS author_id,
                   r.id      AS review_id,
                   COUNT(DISTINCT l.id) FILTER (WHERE l.created_at >= :from) AS likes,
                   COUNT(DISTINCT c.id) FILTER (WHERE c.created_at >= :from) AS comments
            FROM tb_reviews r
            LEFT JOIN tb_review_likes l ON l.review_id = r.id
            LEFT JOIN tb_comments c     ON c.review_id = r.id
            GROUP BY r.user_id, r.id
        )
        SELECT u.id AS "userId",
               COALESCE(SUM(rp.likes * 0.3 + rp.comments * 0.7), 0) AS "reviewScore",
               (SELECT COUNT(*) FROM tb_review_likes l2 WHERE l2.user_id = u.id AND l2.created_at >= :from) AS "givenLikes",
               (SELECT COUNT(*) FROM tb_comments c2     WHERE c2.user_id = u.id AND c2.created_at >= :from) AS "givenComments"
        FROM tb_users u
        LEFT JOIN review_pop rp ON rp.author_id = u.id
        GROUP BY u.id
        """, nativeQuery = true)
    List<UserScoreAggregate> aggregate(@Param("from") Instant from);
}
