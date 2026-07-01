package com.rc.readcompassbatch.repository;

import com.rc.readcompassbatch.domain.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    @Modifying
    @Query(value = "DELETE FROM tb_notifications WHERE confirmed = true AND confirmed_at < :cutoff",
        nativeQuery = true)
    int deleteConfirmedBefore(@Param("cutoff") Instant cutoff);
}
