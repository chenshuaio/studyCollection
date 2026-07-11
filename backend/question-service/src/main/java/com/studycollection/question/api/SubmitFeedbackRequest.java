package com.studycollection.question.api;

import com.studycollection.question.domain.FeedbackType;

public record SubmitFeedbackRequest(
        Long questionId,
        FeedbackType type,
        String content
) {
}
