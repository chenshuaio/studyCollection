package com.studycollection.question.domain;

import java.time.Instant;
import java.util.List;

public record QuestionRevision(
        Long id,
        Long questionId,
        Long feedbackId,
        List<Long> relatedFeedbackIds,
        Long adminUserId,
        String changeSummary,
        String reviewNote,
        QuestionSnapshot beforeQuestion,
        QuestionSnapshot afterQuestion,
        boolean scoringAffected,
        Instant revisedAt
) {
    public QuestionRevision {
        relatedFeedbackIds = relatedFeedbackIds == null ? List.of() : List.copyOf(relatedFeedbackIds);
    }

    public QuestionRevision withId(Long persistedId) {
        return new QuestionRevision(
                persistedId,
                questionId,
                feedbackId,
                relatedFeedbackIds,
                adminUserId,
                changeSummary,
                reviewNote,
                beforeQuestion,
                afterQuestion,
                scoringAffected,
                revisedAt
        );
    }
}
