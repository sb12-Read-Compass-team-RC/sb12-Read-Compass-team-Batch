package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.BookRanking;
import com.rc.readcompassbatch.repository.projection.BookScoreAggregate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRankingRepository extends JpaRepository<BookRanking, UUID> {

    /**
     * 기간 내 도서별 (리뷰수, 평점 평균) 집계.
     * 인기 도서 점수 산출에는 논리 삭제된 리뷰도 포함하므로 is_deleted 필터를 두지 않는다.
     *
     * <p>컬럼 별칭은 {@link BookScoreAggregate} 의 getter 명과 매핑되도록
     * 큰따옴표 카멜케이스로 고정한다(H2 PostgreSQL 모드 / PostgreSQL 모두 대소문자 보존).
     */
    @Query(value = """
        SELECT r.book_id     AS "bookId",
               COUNT(*)      AS "reviewCount",
               AVG(r.rating) AS "ratingAvg"
        FROM tb_reviews r
        WHERE r.created_at >= :from
        GROUP BY r.book_id
        """, nativeQuery = true)
    List<BookScoreAggregate> aggregate(@Param("from") Instant from);
}
