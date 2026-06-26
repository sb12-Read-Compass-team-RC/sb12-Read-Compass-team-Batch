package com.rc.readcompassbatch.batch.config;

import com.rc.readcompassbatch.batch.tasklet.BookRankingTasklet;
import com.rc.readcompassbatch.batch.tasklet.NotificationCleanupTasklet;
import com.rc.readcompassbatch.batch.tasklet.ReviewRankingTasklet;
import com.rc.readcompassbatch.batch.tasklet.UserHardDeleteTasklet;
import com.rc.readcompassbatch.batch.tasklet.UserRankingTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 두 개의 잡으로 구성한다.
 *  1) rankingJob      : 인기 도서 → 인기 리뷰 → 파워 유저 순으로 스냅샷 적재
 *  2) maintenanceJob  : 알림 정리 → 유저 물리 삭제
 *
 * 각 단계는 집계/삭제 쿼리 한 번으로 끝나므로 Tasklet 방식으로 단순하게 구성했다.
 */
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    public static final String RANKING_JOB = "rankingJob";
    public static final String MAINTENANCE_JOB = "maintenanceJob";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final BookRankingTasklet bookRankingTasklet;
    private final ReviewRankingTasklet reviewRankingTasklet;
    private final UserRankingTasklet userRankingTasklet;
    private final NotificationCleanupTasklet notificationCleanupTasklet;
    private final UserHardDeleteTasklet userHardDeleteTasklet;

    // ===== Ranking Job =====
    @Bean
    public Job rankingJob() {
        return new JobBuilder(RANKING_JOB, jobRepository)
            .start(bookRankingStep())
            .next(reviewRankingStep())
            .next(userRankingStep())
            .build();
    }

    @Bean
    public Step bookRankingStep() {
        return new StepBuilder("bookRankingStep", jobRepository)
            .tasklet(bookRankingTasklet, transactionManager)
            .build();
    }

    @Bean
    public Step reviewRankingStep() {
        return new StepBuilder("reviewRankingStep", jobRepository)
            .tasklet(reviewRankingTasklet, transactionManager)
            .build();
    }

    @Bean
    public Step userRankingStep() {
        return new StepBuilder("userRankingStep", jobRepository)
            .tasklet(userRankingTasklet, transactionManager)
            .build();
    }

    // ===== Maintenance Job =====
    @Bean
    public Job maintenanceJob() {
        return new JobBuilder(MAINTENANCE_JOB, jobRepository)
            .start(notificationCleanupStep())
            .next(userHardDeleteStep())
            .build();
    }

    @Bean
    public Step notificationCleanupStep() {
        return new StepBuilder("notificationCleanupStep", jobRepository)
            .tasklet(notificationCleanupTasklet, transactionManager)
            .build();
    }

    @Bean
    public Step userHardDeleteStep() {
        return new StepBuilder("userHardDeleteStep", jobRepository)
            .tasklet(userHardDeleteTasklet, transactionManager)
            .build();
    }
}
