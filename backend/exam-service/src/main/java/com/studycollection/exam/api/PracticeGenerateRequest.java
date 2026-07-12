package com.studycollection.exam.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;

public record PracticeGenerateRequest(
        String knowledgePoint,
        Difficulty difficulty,
        QuestionType type,
        Integer count,
        QuestionBankScope scope
) {
    public PracticeGenerateRequest {
        scope = scope == null ? QuestionBankScope.ALL : scope;
    }

    public PracticeGenerateRequest(
            String knowledgePoint,
            Difficulty difficulty,
            QuestionType type,
            Integer count
    ) {
        this(knowledgePoint, difficulty, type, count, QuestionBankScope.ALL);
    }
}
