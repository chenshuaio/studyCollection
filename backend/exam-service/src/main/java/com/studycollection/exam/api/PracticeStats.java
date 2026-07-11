package com.studycollection.exam.api;

public record PracticeStats(
        Long userId,
        int answeredQuestionCount,
        int gradedQuestionCount,
        int correctQuestionCount
) {
}
