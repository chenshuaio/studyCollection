package com.studycollection.question.domain;

public record Question(
        Long id,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String answer,
        String analysis
) {
}
