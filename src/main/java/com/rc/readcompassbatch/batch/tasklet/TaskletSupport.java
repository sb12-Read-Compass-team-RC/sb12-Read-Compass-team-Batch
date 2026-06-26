package com.rc.readcompassbatch.batch.tasklet;

import java.time.Instant;
import org.springframework.batch.core.scope.context.ChunkContext;

/** 잡 파라미터에서 공통 집계 시각(calculatedAt)을 읽는다. 모든 랭킹 step 이 동일 값을 공유한다. */
final class TaskletSupport {

    private TaskletSupport() {
    }

    static Instant calculatedAt(ChunkContext chunkContext) {
        Object value = chunkContext.getStepContext()
            .getJobParameters()
            .get("calculatedAt");
        if (value instanceof Long epochMilli) {
            return Instant.ofEpochMilli(epochMilli);
        }
        if (value instanceof String s) {
            return Instant.ofEpochMilli(Long.parseLong(s));
        }
        return Instant.now();
    }
}
