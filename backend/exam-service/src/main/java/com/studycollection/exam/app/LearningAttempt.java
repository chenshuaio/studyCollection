package com.studycollection.exam.app;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

import java.time.Instant;

public record LearningAttempt(
        Long id,
        Long userId,
        LearningActivityType activityType,
        String referenceId,
        Long questionId,
        String questionTitle,
        QuestionType questionType,
        Difficulty difficulty,
        String knowledgePoint,
        String submittedAnswer,
        boolean autoGraded,
        Boolean correct,
        int score,
        Instant attemptedAt
) {
    public LearningAttempt {
        submittedAnswer = submittedAnswer == null ? "" : submittedAnswer;
    }

    public LearningAttempt withId(Long persistedId) {
        return new LearningAttempt(
                persistedId,
                userId,
                activityType,
                referenceId,
                questionId,
                questionTitle,
                questionType,
                difficulty,
                knowledgePoint,
                submittedAnswer,
                autoGraded,
                correct,
                score,
                attemptedAt
        );
    }
}
