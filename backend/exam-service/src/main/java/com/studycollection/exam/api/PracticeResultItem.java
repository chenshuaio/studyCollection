package com.studycollection.exam.api;

public record PracticeResultItem(
        Long questionId,
        String submittedAnswer,
        String correctAnswer,
        boolean correct,
        int score,
        String analysis
) {
}
