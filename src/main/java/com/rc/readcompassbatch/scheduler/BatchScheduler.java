package com.rc.readcompassbatch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 상시 구동(long-running) 서비스로 배포할 때 사용하는 내부 스케줄러.
 * 기본값은 매일 새벽에 랭킹/정리 잡을 실행한다.
 * 크론과 타임존은 application.yml 의 batch.scheduler.* 로 조정한다.
 *
 * ECS Scheduled Task(EventBridge cron) 로 "실행 후 종료" 방식으로 배포할 경우에는
 * 이 스케줄러 대신 StartupJobRunner(--batch.job=...)를 사용한다.
 * 그 경우 batch.scheduler.enabled=false 로 끄면 된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "batch.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class BatchScheduler {

    private final BatchJobLauncher launcher;

    /** 랭킹 스냅샷 적재 (기본 매일 03:00). */
    @Scheduled(
        cron = "${batch.scheduler.ranking-cron}",
        zone = "${batch.scheduler.zone:Asia/Seoul}")
    public void scheduleRanking() {
        log.info("[Scheduler] ranking job triggered");
        launcher.runRanking();
    }

    /** 알림/유저 정리 (기본 매일 04:00). */
    @Scheduled(
        cron = "${batch.scheduler.maintenance-cron}",
        zone = "${batch.scheduler.zone:Asia/Seoul}")
    public void scheduleMaintenance() {
        log.info("[Scheduler] maintenance job triggered");
        launcher.runMaintenance();
    }

    /** 카운트 보정 (기본 10분마다). */
    @Scheduled(
            cron = "${batch.scheduler.count-sync-cron}",
            zone = "${batch.scheduler.zone:Asia/Seoul}"
    )
    public void scheduleCountSync() {
        log.info("[Scheduler] count sync job triggered");
        launcher.runCountSync();
    }
}