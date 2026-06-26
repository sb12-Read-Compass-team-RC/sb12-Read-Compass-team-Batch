package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * 확인 완료 후 cutoff 이전에 생성된 알림을 물리 삭제한다.
     * (스키마에 confirmed_at 이 없어 created_at 을 경과 기준으로 사용)
     * 반환: 삭제된 행 수
     */
    @Modifying
    @Query(value = "DELETE FROM tb_notifications WHERE confirmed = true AND created_at < :cutoff",
        nativeQuery = true)
    int deleteConfirmedBefore(@Param("cutoff") Instant cutoff);
}
