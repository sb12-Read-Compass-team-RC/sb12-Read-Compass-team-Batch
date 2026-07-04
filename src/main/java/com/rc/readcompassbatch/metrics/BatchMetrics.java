package com.rc.readcompassbatch.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * 배치 작업 커스텀 메트릭. (심화: Spring Actuator 를 통한 배치 데이터 모니터링)
 * Actuator 엔드포인트(/actuator/metrics, /actuator/prometheus)로 노출된다.
 *  - deokhugam.batch.rankings.saved{type=book|review|user}
 *  - deokhugam.batch.notifications.created
 *  - deokhugam.batch.notifications.deleted
 *  - deokhugam.batch.users.deleted
 */
@Component
public class BatchMetrics {

    private final MeterRegistry registry;

    public BatchMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordRankingsSaved(String type, int count) {
        registry.counter("deokhugam.batch.rankings.saved", "type", type).increment(count);
    }

    public void recordNotificationsCreated(int count) {
        registry.counter("deokhugam.batch.notifications.created").increment(count);
    }

    public void recordNotificationsDeleted(int count) {
        registry.counter("deokhugam.batch.notifications.deleted").increment(count);
    }

    public void recordUsersDeleted(int count) {
        registry.counter("deokhugam.batch.users.deleted").increment(count);
    }

    public void recordCountsSynced(String type, int count) {
        registry.counter("deokhugam.batch.counts.synced", "type", type).increment(count);
    }
}
