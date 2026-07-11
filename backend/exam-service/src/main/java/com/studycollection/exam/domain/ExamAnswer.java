package com.studycollection.exam.domain;

public record ExamAnswer(
        Long questionId,
        String submittedAnswer,
        boolean autoGraded,
        Boolean correct,
        int score
) {
    public ExamAnswer {
        submittedAnswer = submittedAnswer == null ? "" : submittedAnswer;
    }

    public static ExamAnswer saved(Long questionId, String submittedAnswer) {
        return new ExamAnswer(questionId, submittedAnswer, false, null, 0);
    }

    public static ExamAnswer graded(Long questionId, String submittedAnswer, boolean correct, int points) {
        return new ExamAnswer(questionId, submittedAnswer, true, correct, correct ? points : 0);
    }
}
