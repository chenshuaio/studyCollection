package com.studycollection.question.domain;

public record QuestionRevision(
        Long id,
        Long questionId,
        Long feedbackId,
        Long adminUserId,
        String changeSummary,
        String reviewNote
) {
}
