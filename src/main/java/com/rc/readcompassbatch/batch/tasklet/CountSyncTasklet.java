package com.rc.readcompassbatch.batch.tasklet;

import com.rc.readcompassbatch.metrics.BatchMetrics;
import com.rc.readcompassbatch.service.CountSyncResult;
import com.rc.readcompassbatch.service.CountSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CountSyncTasklet implements Tasklet {

    private final CountSyncService countSyncService;
    private final BatchMetrics metrics;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        CountSyncResult result = countSyncService.syncCounts();

        metrics.recordCountsSynced("bookReviewCount", result.bookReviewCountUpdated());
        metrics.recordCountsSynced("bookRating", result.bookRatingUpdated());
        metrics.recordCountsSynced("reviewLikeCount", result.reviewLikeCountUpdated());
        metrics.recordCountsSynced("reviewCommentCount", result.reviewCommentCountUpdated());

        contribution.incrementWriteCount(result.totalUpdated());
        return RepeatStatus.FINISHED;
    }
}