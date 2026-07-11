package com.studycollection.exam.api;

import com.studycollection.exam.domain.ExamAnswer;
import com.studycollection.exam.domain.ExamSession;
import com.studycollection.exam.domain.ExamStatus;

import java.time.Instant;
import java.util.List;

public record ExamSessionResponse(
        Long id,
        String name,
        int durationMinutes,
        String status,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        long remainingSeconds,
        Integer score,
        Integer totalScore,
        List<ExamQuestionResponse> questions
) {
    public static ExamSessionResponse from(ExamSession session, Instant now) {
        boolean submitted = session.status() == ExamStatus.SUBMITTED;
        List<ExamQuestionResponse> questions = session.questions().stream()
                .map(question -> {
                    ExamAnswer answer = session.answers().getOrDefault(
                            question.questionId(),
                            ExamAnswer.saved(question.questionId(), "")
                    );
                    return new ExamQuestionResponse(
                            question.questionId(),
                            question.title(),
                            question.type().name(),
                            question.difficulty().name(),
                            question.knowledgePoint(),
                            answer.submittedAnswer(),
                            answer.autoGraded(),
                            answer.correct(),
                            answer.score(),
                            submitted ? question.correctAnswer() : "",
                            submitted && question.analysis() != null ? question.analysis() : ""
                    );
                })
                .toList();
        return new ExamSessionResponse(
                session.id(),
                session.name(),
                session.durationMinutes(),
                session.status().name(),
                session.startedAt(),
                session.expiresAt(),
                session.submittedAt(),
                session.remainingSeconds(now),
                session.score(),
                session.totalScore(),
                questions
        );
    }
}
