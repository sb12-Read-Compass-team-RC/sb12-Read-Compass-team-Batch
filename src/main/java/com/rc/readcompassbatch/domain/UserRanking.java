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
 * 파워 유저 랭킹 스냅샷.
 * 활동 점수 = (작성 리뷰의 인기 점수 합 * 0.5) + (참여한 좋아요 수 * 0.2) + (참여한 댓글 수 * 0.3)
 */
@Entity
@Table(name = "tb_user_rankings")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRanking extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID userId;

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
