package com.rc.readcompassbatch.batch.tasklet;

import com.rc.readcompassbatch.metrics.BatchMetrics;
import com.rc.readcompassbatch.service.MaintenanceService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanupTasklet implements Tasklet {

    private final MaintenanceService maintenanceService;
    private final BatchMetrics metrics;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int deleted = maintenanceService.cleanupConfirmedNotifications(Instant.now());
        metrics.recordNotificationsDeleted(deleted);
        contribution.incrementWriteCount(deleted);
        return RepeatStatus.FINISHED;
    }
}
