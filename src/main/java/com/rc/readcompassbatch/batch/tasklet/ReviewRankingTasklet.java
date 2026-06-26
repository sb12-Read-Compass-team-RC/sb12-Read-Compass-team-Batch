package com.rc.readcompassbatch.batch.tasklet;

import com.rc.readcompassbatch.metrics.BatchMetrics;
import com.rc.readcompassbatch.service.RankingResult;
import com.rc.readcompassbatch.service.RankingService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewRankingTasklet implements Tasklet {

    private final RankingService rankingService;
    private final BatchMetrics metrics;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        Instant calculatedAt = TaskletSupport.calculatedAt(chunkContext);
        RankingResult result = rankingService.calculateReviewRankings(calculatedAt);
        metrics.recordRankingsSaved("review", result.rankingsSaved());
        metrics.recordNotificationsCreated(result.notificationsCreated());
        contribution.incrementWriteCount(result.rankingsSaved());
        return RepeatStatus.FINISHED;
    }
}
