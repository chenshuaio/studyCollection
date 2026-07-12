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
        QuestionBankScope targetScope,
        PendingQuestionStatus status
) {
    public PendingQuestion {
        targetScope = targetScope == null ? QuestionBankScope.PUBLIC : targetScope;
        if (targetScope == QuestionBankScope.ALL) {
            throw new IllegalArgumentException("待审核题目目标范围仅支持公共题库或个人题库");
        }
    }

    public PendingQuestion(
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
        this(
                id,
                submitterUserId,
                title,
                type,
                difficulty,
                knowledgePoint,
                answer,
                analysis,
                QuestionBankScope.PUBLIC,
                status
        );
    }

    public Question toQuestion() {
        Long ownerUserId = targetScope == QuestionBankScope.PERSONAL ? submitterUserId : null;
        return new Question(null, ownerUserId, title, type, difficulty, knowledgePoint, answer, analysis);
    }

    public PendingQuestion withStatus(PendingQuestionStatus nextStatus) {
        return new PendingQuestion(
                id,
                submitterUserId,
                title,
                type,
                difficulty,
                knowledgePoint,
                answer,
                analysis,
                targetScope,
                nextStatus
        );
    }
}
