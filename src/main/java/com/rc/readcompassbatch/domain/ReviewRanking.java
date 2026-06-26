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
 * 인기 리뷰 랭킹 스냅샷.
 * 점수 = (기간 좋아요 수 * 0.3) + (기간 댓글 수 * 0.7)
 * TOP 10 진입 시 리뷰 작성자에게 알림을 생성한다.
 */
@Entity
@Table(name = "tb_review_rankings")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewRanking extends BaseEntity {

    @Column(name = "review_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID reviewId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20, updatable = false)
    private PeriodType periodType;

    @Column(name = "rank_position", nullable = false, updatable = false)
    private int rankPosition;

    @Column(nullable = false, precision = 10, scale = 2, updatable = false)
    private BigDecimal score;

    @Column(name = "calculated_at", nullable = false, updatable = false)
    private Instant calculatedAt;
}
