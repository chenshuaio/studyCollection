package com.studycollection.exam.api;

public record PracticeAnswer(Long questionId, String answer, String correctAnswer, String analysis) {
    public PracticeAnswer(Long questionId, String answer) {
        this(questionId, answer, null, null);
    }
}
