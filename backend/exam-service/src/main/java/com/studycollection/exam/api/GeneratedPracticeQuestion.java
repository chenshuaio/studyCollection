package com.studycollection.exam.api;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

public record GeneratedPracticeQuestion(
        Long id,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint
) {
}
