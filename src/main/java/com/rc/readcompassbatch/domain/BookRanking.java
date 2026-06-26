package com.rc.readcompassbatch.domain;

import com.rc.readcompassbatch.common.PeriodType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 인기 도서 랭킹 스냅샷. 매일 배치가 calculated_at 단위로 새로 적재한다.
 * 점수 = (기간 리뷰수 * 0.4) + (기간 평점 평균 * 0.6)
 */
@Entity
@Table(name = "tb_book_rankings")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookRanking extends BaseEntity {

    @Column(name = "book_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID bookId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private PeriodType periodType;

    @Column(name = "rank_position", nullable = false)
    private int rankPosition;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;
}
