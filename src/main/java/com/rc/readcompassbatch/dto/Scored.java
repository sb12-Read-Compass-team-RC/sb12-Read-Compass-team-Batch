package com.rc.readcompassbatch.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 점수 산출 중간 결과: 랭킹 대상 ID 와 계산된 점수.
 * 도서/리뷰/유저 랭킹 모두 (대상 ID, 점수) 구조가 동일하므로 공용으로 사용한다.
 *
 * @param id    랭킹 대상 식별자 (bookId / reviewId / userId)
 * @param score 산출된 점수 (소수점 2자리)
 */
public record Scored(UUID id, BigDecimal score) {}
