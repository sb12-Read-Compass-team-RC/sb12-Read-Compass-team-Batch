package com.rc.readcompassbatch.repository.projection;

import java.util.UUID;

/**
 * 인기 리뷰 점수 산출용 기간 집계 결과 (Spring Data 인터페이스 프로젝션).
 * {@code ReviewRankingRepository#aggregate} 의 SELECT 별칭과 getter 명이 1:1로 매핑된다.
 *
 * <p>집계값은 DB/드라이버에 따라 구체 타입이 달라지므로 {@link Number} 로 받아 호출부에서 변환한다.
 */
public interface ReviewScoreAggregate {

    UUID getReviewId();

    /** 기간 내 좋아요 수. */
    Number getLikeCount();

    /** 기간 내 댓글 수. */
    Number getCommentCount();
}
