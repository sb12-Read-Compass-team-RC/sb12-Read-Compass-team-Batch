package com.rc.readcompassbatch.service;

import com.rc.readcompassbatch.dto.CountMismatchDetail;
import com.rc.readcompassbatch.repository.CountSyncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CountSyncService {

    private final CountSyncRepository countSyncRepository;

    @Transactional
    public CountSyncResult syncCounts() {
        logMismatchDetails();

        int bookReviewCountUpdated = countSyncRepository.syncBookReviewCount();
        int bookRatingUpdated = countSyncRepository.syncBookRating();
        int reviewLikeCountUpdated = countSyncRepository.syncReviewLikeCount();
        int reviewCommentCountUpdated = countSyncRepository.syncReviewCommentCount();

        CountSyncResult result = new CountSyncResult(
                bookReviewCountUpdated,
                bookRatingUpdated,
                reviewLikeCountUpdated,
                reviewCommentCountUpdated
        );

        log.info("[CountSync] result={}", result);
        return result;
    }

    private void logMismatchDetails() {
        List<CountMismatchDetail> bookReviewMismatches =
                countSyncRepository.findBookReviewCountMismatches();

        List<CountMismatchDetail> reviewLikeMismatches =
                countSyncRepository.findReviewLikeCountMismatches();

        List<CountMismatchDetail> reviewCommentMismatches =
                countSyncRepository.findReviewCommentCountMismatches();

        logMismatchList("book.review_cnt", bookReviewMismatches);
        logMismatchList("review.like_cnt", reviewLikeMismatches);
        logMismatchList("review.comment_cnt", reviewCommentMismatches);
    }

    private void logMismatchList(String label, List<CountMismatchDetail> mismatches) {
        if (mismatches.isEmpty()) {
            log.info("[CountSync] {} mismatch 없음", label);
            return;
        }

        log.warn("[CountSync] {} mismatch {}건 발견", label, mismatches.size());

        for (CountMismatchDetail mismatch : mismatches) {
            log.warn(
                    "[CountSync] type={}, targetId={}, savedCount={}, actualCount={}",
                    mismatch.type(),
                    mismatch.targetId(),
                    mismatch.savedCount(),
                    mismatch.actualCount()
            );
        }
    }
}