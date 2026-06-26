package com.rc.readcompassbatch.scheduler;

import com.rc.readcompassbatch.batch.config.BatchConfig;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.stereotype.Component;

/**
 * 잡 실행 진입점. Spring Batch 6 부터 JobLauncher 는 JobOperator 로 대체되었다.
 * 매 실행마다 고유한 run.id 와 공통 집계 시각(calculatedAt) 파라미터를 부여한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobLauncher {

    private final JobOperator jobOperator;
    private final Job rankingJob;
    private final Job maintenanceJob;

    public void runRanking() {
        run(rankingJob);
    }

    public void runMaintenance() {
        run(maintenanceJob);
    }

    private void run(Job job) {
        try {
            JobParameters params = new JobParametersBuilder()
                .addLong("calculatedAt", Instant.now().toEpochMilli())
                .addLong("run.id", System.nanoTime())
                .toJobParameters();
            JobExecution execution = jobOperator.start(job, params);
            log.info("[Batch] job={} status={}", job.getName(), execution.getStatus());
        } catch (Exception e) {
            log.error("[Batch] job={} failed", job.getName(), e);
        }
    }

    /** 잡 이름으로 실행 (ECS Scheduled Task / 수동 실행용). */
    public void runByName(String jobName) {
        switch (jobName) {
            case BatchConfig.RANKING_JOB -> runRanking();
            case BatchConfig.MAINTENANCE_JOB -> runMaintenance();
            default -> log.warn("[Batch] unknown job name: {}", jobName);
        }
    }
}
