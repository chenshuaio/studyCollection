package com.studycollection.mistake.domain;

import com.studycollection.question.domain.QuestionType;

import java.time.Instant;

public record MistakeFilter(
        String knowledgePoint,
        QuestionType questionType,
        String status,
        Instant wrongFrom,
        Instant wrongToExclusive
) {
    public boolean matches(MistakeRecord record) {
        return (isBlank(knowledgePoint) || knowledgePoint.equals(record.knowledgePoint()))
                && (questionType == null || questionType == record.questionType())
                && (isBlank(status) || status.equals(record.status()))
                && (wrongFrom == null || !record.lastWrongAt().isBefore(wrongFrom))
                && (wrongToExclusive == null || record.lastWrongAt().isBefore(wrongToExclusive));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
