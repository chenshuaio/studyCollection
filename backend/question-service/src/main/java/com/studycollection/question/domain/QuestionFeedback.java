package com.studycollection.question.domain;

public record QuestionFeedback(
        Long id,
        Long userId,
        Long questionId,
        FeedbackType type,
        String content,
        FeedbackStatus status
) {
}
