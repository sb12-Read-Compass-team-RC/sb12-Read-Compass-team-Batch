package com.rc.readcompassbatch.repository.projection;

import java.util.UUID;

/**
 * 인기 도서 점수 산출용 기간 집계 결과 (Spring Data 인터페이스 프로젝션).
 * {@code BookRankingRepository#aggregate} 의 SELECT 별칭과 getter 명이 1:1로 매핑된다.
 *
 * <p>네이티브 쿼리 집계값의 구체 타입(COUNT→Long/BigInteger, AVG→BigDecimal 등)은
 * DB/드라이버에 따라 달라지므로 {@link Number} 로 받아 호출부에서 변환한다.
 * 평점 평균은 그룹 내 평점이 모두 NULL 이면 NULL 일 수 있으므로 nullable 이다.
 */
public interface BookScoreAggregate {

    UUID getBookId();

    /** 기간 내 리뷰 수. */
    Number getReviewCount();

    /** 기간 내 평점 평균 (평점이 전부 NULL 이면 null). */
    Number getRatingAvg();
}
