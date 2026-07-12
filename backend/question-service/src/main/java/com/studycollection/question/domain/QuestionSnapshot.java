package com.studycollection.question.domain;

public record QuestionSnapshot(
        Long id,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String answer,
        String analysis,
        String source
) {
    public static QuestionSnapshot from(Question question, String source) {
        return new QuestionSnapshot(
                question.id(),
                question.title(),
                question.type(),
                question.difficulty(),
                question.knowledgePoint(),
                question.answer(),
                question.analysis(),
                source
        );
    }
}
