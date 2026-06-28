package com.studycollection.question.domain;

public record PendingQuestion(
        Long id,
        Long submitterUserId,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String answer,
        String analysis,
        PendingQuestionStatus status
) {
    public Question toQuestion() {
        return new Question(null, title, type, difficulty, knowledgePoint, answer, analysis);
    }

    public PendingQuestion withStatus(PendingQuestionStatus nextStatus) {
        return new PendingQuestion(id, submitterUserId, title, type, difficulty, knowledgePoint, answer, analysis, nextStatus);
    }
}
