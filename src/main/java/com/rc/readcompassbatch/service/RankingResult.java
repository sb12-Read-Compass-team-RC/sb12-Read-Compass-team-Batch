package com.rc.readcompassbatch.service;

/** 랭킹 적재 결과 요약(메트릭/로그용). */
public record RankingResult(int rankingsSaved, int notificationsCreated) {
    public static RankingResult of(int rankingsSaved) {
        return new RankingResult(rankingsSaved, 0);
    }
}
