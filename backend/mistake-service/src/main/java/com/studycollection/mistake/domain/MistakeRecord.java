package com.studycollection.mistake.domain;

import com.studycollection.question.domain.QuestionType;

import java.time.Instant;

public record MistakeRecord(
        Long userId,
        Long questionId,
        String questionTitle,
        QuestionType questionType,
        String knowledgePoint,
        String lastSubmittedAnswer,
        String sourceContext,
        String status,
        int wrongCount,
        Instant firstWrongAt,
        Instant lastWrongAt
) {
}
