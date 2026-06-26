package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.BookRanking;
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
     * 반환: [0]=book_id(UUID), [1]=review_count(Number), [2]=rating_avg(Number)
     */
    @Query(value = """
        SELECT r.book_id        AS book_id,
               COUNT(*)         AS review_count,
               AVG(r.rating)    AS rating_avg
        FROM tb_reviews r
        WHERE r.created_at >= :from
        GROUP BY r.book_id
        """, nativeQuery = true)
    List<Object[]> aggregate(@Param("from") Instant from);
}
