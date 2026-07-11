package com.studycollection.exam.api;

public record PracticeResultItem(
        Long questionId,
        String submittedAnswer,
        String correctAnswer,
        boolean autoGraded,
        Boolean correct,
        int score,
        String analysis
) {
}
