package com.studycollection.question.domain;

public record Question(
        Long id,
        Long ownerUserId,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String answer,
        String analysis
) {
    public Question(
            Long id,
            String title,
            QuestionType type,
            Difficulty difficulty,
            String knowledgePoint,
            String answer,
            String analysis
    ) {
        this(id, null, title, type, difficulty, knowledgePoint, answer, analysis);
    }

    public boolean isPublic() {
        return ownerUserId == null;
    }

    public boolean isOwnedBy(Long userId) {
        return ownerUserId != null && ownerUserId.equals(userId);
    }
}
