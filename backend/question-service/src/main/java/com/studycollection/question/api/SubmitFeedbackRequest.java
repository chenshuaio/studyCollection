package com.studycollection.question.api;

import com.studycollection.question.domain.FeedbackType;

public record SubmitFeedbackRequest(
        Long questionId,
        FeedbackType type,
        String content,
        String submittedAnswer,
        String sourceContext,
        String sourceReference
) {
    public SubmitFeedbackRequest(Long questionId, FeedbackType type, String content) {
        this(questionId, type, content, "", "UNKNOWN", "");
    }
}
