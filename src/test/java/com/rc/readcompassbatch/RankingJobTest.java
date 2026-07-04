package com.rc.readcompassbatch;

import static org.assertj.core.api.Assertions.assertThat;

import com.rc.readcompassbatch.repository.BookRankingRepository;
import com.rc.readcompassbatch.repository.NotificationRepository;
import com.rc.readcompassbatch.repository.ReviewRankingRepository;
import com.rc.readcompassbatch.repository.UserRankingRepository;
import com.rc.readcompassbatch.service.MaintenanceService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 배치 단독 검증 예시.
 *
 * 핵심: 메인 프로젝트(read-compass)를 전혀 실행하지 않는다.
 * 배치 애플리케이션 컨텍스트만 띄우고, 시드 데이터를 넣은 뒤 잡을 프로그램으로 실행해 검증한다.
 * (실제 운영에서는 같은 RDB 를 공유하지만, 테스트에서는 인메모리 DB 를 사용한다.)
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class RankingJobTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    private Job rankingJob;

    @Autowired
    private BookRankingRepository bookRankingRepository;
    @Autowired
    private ReviewRankingRepository reviewRankingRepository;
    @Autowired
    private UserRankingRepository userRankingRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private MaintenanceService maintenanceService;

    @Test
    void rankingJob_적재_및_TOP10_알림생성() throws Exception {
        jobOperatorTestUtils.setJob(rankingJob);

        JobParameters params = new JobParametersBuilder()
            .addLong("calculatedAt", Instant.now().toEpochMilli())
            .addLong("run.id", System.nanoTime())
            .toJobParameters();

        JobExecution execution = jobOperatorTestUtils.startJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // 도서/리뷰/유저 랭킹 스냅샷이 적재되어야 한다.
        assertThat(bookRankingRepository.count()).isGreaterThan(0);
        assertThat(reviewRankingRepository.count()).isGreaterThan(0);
        assertThat(userRankingRepository.count()).isGreaterThan(0);
        // 리뷰1 이 TOP 10 에 들었으므로 REVIEW_RANKED 알림이 생성된다.
        assertThat(notificationRepository.count()).isGreaterThan(1);
    }

    @Test
    void maintenance_오래된_확인알림_삭제() {
        int deleted = maintenanceService.cleanupConfirmedNotifications(Instant.now());
        assertThat(deleted).isGreaterThanOrEqualTo(1);
    }

    @Test
    void maintenance_논리삭제_1일경과_유저_물리삭제() {
        int deleted = maintenanceService.hardDeleteUsers(Instant.now());
        assertThat(deleted).isGreaterThanOrEqualTo(1);
    }
}
