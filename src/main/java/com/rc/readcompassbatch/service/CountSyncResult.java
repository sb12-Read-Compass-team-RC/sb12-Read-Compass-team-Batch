package com.rc.readcompassbatch.service;

public record CountSyncResult(
        int bookReviewCountUpdated,
        int bookRatingUpdated,
        int reviewLikeCountUpdated,
        int reviewCommentCountUpdated
) {
    public int totalUpdated() {
        return bookReviewCountUpdated + bookRatingUpdated
                + reviewLikeCountUpdated + reviewCommentCountUpdated;
    }
}