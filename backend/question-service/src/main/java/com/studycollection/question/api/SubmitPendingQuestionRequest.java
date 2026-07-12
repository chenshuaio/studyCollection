package com.studycollection.question.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionBankScope;
import com.studycollection.question.domain.QuestionType;

public record SubmitPendingQuestionRequest(
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String answer,
        String analysis,
        QuestionBankScope targetScope
) {
    public SubmitPendingQuestionRequest {
        targetScope = targetScope == null ? QuestionBankScope.PUBLIC : targetScope;
        if (targetScope == QuestionBankScope.ALL) {
            throw new IllegalArgumentException("导入目标仅支持公共题库或个人题库");
        }
    }

    public SubmitPendingQuestionRequest(
            String title,
            QuestionType type,
            Difficulty difficulty,
            String knowledgePoint,
            String answer,
            String analysis
    ) {
        this(title, type, difficulty, knowledgePoint, answer, analysis, QuestionBankScope.PUBLIC);
    }
}
