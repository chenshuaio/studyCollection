package com.studycollection.exam.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

public record PracticeGenerateRequest(
        String knowledgePoint,
        Difficulty difficulty,
        QuestionType type,
        Integer count
) {
}
