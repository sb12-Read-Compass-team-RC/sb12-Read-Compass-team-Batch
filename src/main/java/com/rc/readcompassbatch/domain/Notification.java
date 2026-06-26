package com.rc.readcompassbatch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 알림. 배치는 인기 리뷰 TOP 10 진입(REVIEW_RANKED) 알림을 생성하고,
 * 확인 후 1주일이 지난 알림을 물리 삭제한다.
 */
@Entity
@Table(name = "tb_notifications")
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "review_id", nullable = false, columnDefinition = "uuid")
    private UUID reviewId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "noti_type", nullable = false, length = 30)
    private NotificationType notiType;

    @Builder.Default
    @Column(nullable = false)
    private boolean confirmed = false;
}
