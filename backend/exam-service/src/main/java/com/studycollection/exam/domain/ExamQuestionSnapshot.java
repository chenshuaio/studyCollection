package com.studycollection.exam.domain;

import com.studycollection.question.domain.Difficulty;
import com.studycollection.question.domain.QuestionType;

public record ExamQuestionSnapshot(
        Long questionId,
        String title,
        QuestionType type,
        Difficulty difficulty,
        String knowledgePoint,
        String correctAnswer,
        String analysis,
        int sortOrder
) {
    public boolean isObjective() {
        return switch (type) {
            case SINGLE_CHOICE, MULTIPLE_CHOICE, TRUE_FALSE, FILL_BLANK -> true;
            case SHORT_ANSWER, PROGRAMMING -> false;
        };
    }
}
