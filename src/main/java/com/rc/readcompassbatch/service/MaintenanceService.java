package com.rc.readcompassbatch.service;

import com.rc.readcompassbatch.repository.NotificationRepository;
import com.rc.readcompassbatch.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정리(maintenance) 배치.
 *  - 확인 완료 후 1주일 경과 알림 물리 삭제
 *  - 논리 삭제 후 1일 경과 유저 물리 삭제 (연관 데이터는 FK CASCADE 로 함께 삭제)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /** 운영 기준: 7일. 프로토타입처럼 짧게 바꾸려면 프로퍼티로 조정. */
    @Value("${batch.cleanup.notification-retention-days:7}")
    private long notificationRetentionDays;

    /** 운영 기준: 1일. */
    @Value("${batch.cleanup.user-grace-days:1}")
    private long userGraceDays;

    @Transactional
    public int cleanupConfirmedNotifications(Instant now) {
        Instant cutoff = now.minus(notificationRetentionDays, ChronoUnit.DAYS);
        int deleted = notificationRepository.deleteConfirmedBefore(cutoff);
        log.info("[NotificationCleanup] cutoff={} deleted={}", cutoff, deleted);
        return deleted;
    }

    @Transactional
    public int hardDeleteUsers(Instant now) {
        Instant cutoff = now.minus(userGraceDays, ChronoUnit.DAYS);
        int target = userRepository.countHardDeletable(cutoff);
        int deleted = userRepository.deleteSoftDeletedBefore(cutoff);
        log.info("[UserHardDelete] cutoff={} target={} deleted={}", cutoff, target, deleted);
        return deleted;
    }
}
