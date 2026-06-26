package com.rc.readcompassbatch.service;

import com.rc.readcompassbatch.common.PeriodType;
import com.rc.readcompassbatch.domain.BookRanking;
import com.rc.readcompassbatch.domain.Notification;
import com.rc.readcompassbatch.domain.NotificationType;
import com.rc.readcompassbatch.domain.ReviewRanking;
import com.rc.readcompassbatch.domain.UserRanking;
import com.rc.readcompassbatch.repository.BookRankingRepository;
import com.rc.readcompassbatch.repository.NotificationRepository;
import com.rc.readcompassbatch.repository.ReviewRankingRepository;
import com.rc.readcompassbatch.repository.UserRankingRepository;
import com.rc.readcompassbatch.repository.projection.BookScoreAggregate;
import com.rc.readcompassbatch.repository.projection.ReviewScoreAggregate;
import com.rc.readcompassbatch.repository.projection.UserScoreAggregate;
import com.rc.readcompassbatch.dto.Scored;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 랭킹(인기 도서/인기 리뷰/파워 유저) 스냅샷을 계산해 적재한다.
 * 한 번의 배치 실행에서 네 기간(DAILY/WEEKLY/MONTHLY/ALL_TIME)을 모두 적재하며,
 * 동일 실행의 모든 스냅샷은 같은 calculatedAt 값을 공유한다.
 * 조회(메인 프로젝트)는 period_type 별 최신 calculated_at 으로 데이터를 읽는다.
 *
 * 적재 건수와 알림 발송 순위는 application.yml 의 batch.ranking.* 로 조정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final BookRankingRepository bookRankingRepository;
    private final ReviewRankingRepository reviewRankingRepository;
    private final UserRankingRepository userRankingRepository;
    private final NotificationRepository notificationRepository;

    /** 인기 도서 적재 상위 N건 (점수 0 이하/활동 없는 항목은 제외). */
    @Value("${batch.ranking.book-top-n:100}")
    private int bookTopN;

    /** 인기 리뷰 적재 상위 N건. */
    @Value("${batch.ranking.review-top-n:100}")
    private int reviewTopN;

    /** 파워 유저 적재 상위 N건. */
    @Value("${batch.ranking.user-top-n:100}")
    private int userTopN;

    /** 인기 리뷰 알림을 보낼 상위 순위 (이 순위 이내 진입 시 작성자에게 알림). */
    @Value("${batch.ranking.notify-top-rank:10}")
    private int notifyTopRank;

    // ---------------------------------------------------------------
    // 인기 도서
    // 점수 = (기간 리뷰수 * 0.4) + (기간 평점 평균 * 0.6)
    // ---------------------------------------------------------------
    @Transactional
    public RankingResult calculateBookRankings(Instant calculatedAt) {
        int saved = 0;
        for (PeriodType period : PeriodType.values()) {
            List<BookScoreAggregate> rows = bookRankingRepository.aggregate(period.from(calculatedAt));

            List<Scored> scored = new ArrayList<>();
            for (BookScoreAggregate row : rows) {
                long reviewCnt = num(row.getReviewCount()).longValue();
                double ratingAvg = num(row.getRatingAvg()).doubleValue();
                BigDecimal score = scale(reviewCnt * 0.4 + ratingAvg * 0.6);
                if (score.signum() > 0) {
                    scored.add(new Scored(row.getBookId(), score));
                }
            }
            scored.sort(Comparator.comparing(Scored::score).reversed());

            List<BookRanking> snapshot = new ArrayList<>();
            int rank = 1;
            for (Scored s : scored) {
                if (rank > bookTopN) break;
                snapshot.add(BookRanking.builder()
                    .bookId(s.id())
                    .periodType(period)
                    .rankPosition(rank++)
                    .score(s.score())
                    .calculatedAt(calculatedAt)
                    .build());
            }
            bookRankingRepository.saveAll(snapshot);
            saved += snapshot.size();
            log.info("[BookRanking] period={} saved={}", period, snapshot.size());
        }
        return RankingResult.of(saved);
    }

    // ---------------------------------------------------------------
    // 인기 리뷰
    // 점수 = (기간 좋아요 수 * 0.3) + (기간 댓글 수 * 0.7)
    // notifyTopRank 이내 진입 시 작성자 알림 생성
    // ---------------------------------------------------------------
    @Transactional
    public RankingResult calculateReviewRankings(Instant calculatedAt) {
        int saved = 0;
        int notified = 0;
        for (PeriodType period : PeriodType.values()) {
            List<ReviewScoreAggregate> rows = reviewRankingRepository.aggregate(period.from(calculatedAt));

            List<Scored> scored = new ArrayList<>();
            for (ReviewScoreAggregate row : rows) {
                long likeCnt = num(row.getLikeCount()).longValue();
                long commentCnt = num(row.getCommentCount()).longValue();
                BigDecimal score = scale(likeCnt * 0.3 + commentCnt * 0.7);
                if (score.signum() > 0) {
                    scored.add(new Scored(row.getReviewId(), score));
                }
            }
            scored.sort(Comparator.comparing(Scored::score).reversed());

            List<ReviewRanking> snapshot = new ArrayList<>();
            int rank = 1;
            for (Scored s : scored) {
                if (rank > reviewTopN) break;
                int rankPosition = rank++;
                snapshot.add(ReviewRanking.builder()
                    .reviewId(s.id())
                    .periodType(period)
                    .rankPosition(rankPosition)
                    .score(s.score())
                    .calculatedAt(calculatedAt)
                    .build());

                if (rankPosition <= notifyTopRank) {
                    notified += notifyRanked(s.id(), period, rankPosition);
                }
            }
            reviewRankingRepository.saveAll(snapshot);
            saved += snapshot.size();
            log.info("[ReviewRanking] period={} saved={}", period, snapshot.size());
        }
        return new RankingResult(saved, notified);
    }

    private int notifyRanked(UUID reviewId, PeriodType period, int rank) {
        UUID authorId = reviewRankingRepository.findAuthorId(reviewId);
        if (authorId == null) {
            return 0;
        }
        String message = "작성하신 리뷰가 %s 인기 리뷰 %d위에 선정되었습니다.".formatted(periodLabel(period), rank);
        notificationRepository.save(Notification.builder()
            .userId(authorId)
            .reviewId(reviewId)
            .message(message)
            .notiType(NotificationType.REVIEW_RANKED)
            .confirmed(false)
            .build());
        return 1;
    }

    // ---------------------------------------------------------------
    // 파워 유저
    // 활동 점수 = (작성 리뷰 인기 점수 합 * 0.5) + (참여 좋아요 수 * 0.2) + (참여 댓글 수 * 0.3)
    // ---------------------------------------------------------------
    @Transactional
    public RankingResult calculateUserRankings(Instant calculatedAt) {
        int saved = 0;
        for (PeriodType period : PeriodType.values()) {
            List<UserScoreAggregate> rows = userRankingRepository.aggregate(period.from(calculatedAt));

            List<Scored> scored = new ArrayList<>();
            for (UserScoreAggregate row : rows) {
                double reviewScore = num(row.getReviewScore()).doubleValue();
                long givenLikes = num(row.getGivenLikes()).longValue();
                long givenComments = num(row.getGivenComments()).longValue();
                BigDecimal score = scale(reviewScore * 0.5 + givenLikes * 0.2 + givenComments * 0.3);
                if (score.signum() > 0) {
                    scored.add(new Scored(row.getUserId(), score));
                }
            }
            scored.sort(Comparator.comparing(Scored::score).reversed());

            List<UserRanking> snapshot = new ArrayList<>();
            int rank = 1;
            for (Scored s : scored) {
                if (rank > userTopN) break;
                snapshot.add(UserRanking.builder()
                    .userId(s.id())
                    .periodType(period)
                    .rankPosition(rank++)
                    .score(s.score())
                    .calculatedAt(calculatedAt)
                    .build());
            }
            userRankingRepository.saveAll(snapshot);
            saved += snapshot.size();
            log.info("[UserRanking] period={} saved={}", period, snapshot.size());
        }
        return RankingResult.of(saved);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------
    private static Number num(Number n) {
        return n == null ? 0L : n;
    }

    private static BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static String periodLabel(PeriodType period) {
        return switch (period) {
            case DAILY -> "일간";
            case WEEKLY -> "주간";
            case MONTHLY -> "월간";
            case ALL_TIME -> "역대";
        };
    }
}