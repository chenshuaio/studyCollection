package com.studycollection.exam.api;

public record ExamQuestionResponse(
        Long id,
        String title,
        String type,
        String difficulty,
        String knowledgePoint,
        String submittedAnswer,
        boolean autoGraded,
        Boolean correct,
        int score,
        String correctAnswer,
        String analysis
) {
}
