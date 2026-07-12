package com.studycollection.question.domain;

import java.time.Instant;

public record QuestionFeedback(
        Long id,
        Long userId,
        Long questionId,
        FeedbackType type,
        String content,
        String submittedAnswer,
        String sourceContext,
        String sourceReference,
        FeedbackStatus status,
        Instant createdAt,
        Long reviewedBy,
        String reviewNote,
        Instant reviewedAt
) {
    public QuestionFeedback {
        submittedAnswer = submittedAnswer == null ? "" : submittedAnswer;
        sourceContext = sourceContext == null || sourceContext.isBlank() ? "UNKNOWN" : sourceContext;
        sourceReference = sourceReference == null ? "" : sourceReference;
        reviewNote = reviewNote == null ? "" : reviewNote;
    }

    public QuestionFeedback withId(Long persistedId) {
        return new QuestionFeedback(
                persistedId,
                userId,
                questionId,
                type,
                content,
                submittedAnswer,
                sourceContext,
                sourceReference,
                status,
                createdAt,
                reviewedBy,
                reviewNote,
                reviewedAt
        );
    }

    public QuestionFeedback reviewed(
            FeedbackStatus reviewedStatus,
            Long adminUserId,
            String note,
            Instant time
    ) {
        return new QuestionFeedback(
                id,
                userId,
                questionId,
                type,
                content,
                submittedAnswer,
                sourceContext,
                sourceReference,
                reviewedStatus,
                createdAt,
                adminUserId,
                note,
                time
        );
    }
}
