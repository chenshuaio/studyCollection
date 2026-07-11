package com.studycollection.question.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

public record SubmitPendingQuestionRequest(
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String answer,
        String analysis
) {
}
