package com.rc.readcompassbatch.repository.projection;

import java.util.UUID;

/**
 * 파워 유저 점수 산출용 기간 집계 결과 (Spring Data 인터페이스 프로젝션).
 * {@code UserRankingRepository#aggregate} 의 SELECT 별칭과 getter 명이 1:1로 매핑된다.
 *
 * <p>집계값은 DB/드라이버에 따라 구체 타입이 달라지므로 {@link Number} 로 받아 호출부에서 변환한다.
 */
public interface UserScoreAggregate {

    UUID getUserId();

    /** 작성 리뷰들의 인기 점수 합 = Σ(기간 좋아요*0.3 + 기간 댓글*0.7). */
    Number getReviewScore();

    /** 유저가 누른 좋아요 수. */
    Number getGivenLikes();

    /** 유저가 작성한 댓글 수. */
    Number getGivenComments();
}
