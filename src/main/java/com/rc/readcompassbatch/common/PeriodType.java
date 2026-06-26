package com.rc.readcompassbatch.common;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 랭킹 집계 기간. 메인 프로젝트의 com.rc.readcompass.common.PeriodType 와 동일하다.
 * 각 기간은 "집계 시점(now) 기준 롤링 윈도우"로 해석한다.
 */
public enum PeriodType {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME;

    /** 해당 기간의 집계 시작 시각. ALL_TIME 은 전체 구간을 의미하는 EPOCH 를 반환한다. */
    public Instant from(Instant now) {
        return switch (this) {
            case DAILY -> now.minus(1, ChronoUnit.DAYS);
            case WEEKLY -> now.minus(7, ChronoUnit.DAYS);
            case MONTHLY -> now.minus(30, ChronoUnit.DAYS);
            case ALL_TIME -> Instant.EPOCH;
        };
    }
}
