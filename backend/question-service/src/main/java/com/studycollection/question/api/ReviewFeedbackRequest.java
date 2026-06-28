package com.studycollection.question.api;

public record ReviewFeedbackRequest(
        Long adminUserId,
        String reviewNote
) {
}
