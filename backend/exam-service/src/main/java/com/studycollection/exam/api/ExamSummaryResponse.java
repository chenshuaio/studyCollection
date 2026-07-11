package com.studycollection.exam.api;

import com.studycollection.exam.domain.ExamAnswer;
import com.studycollection.exam.domain.ExamSession;

import java.time.Instant;

public record ExamSummaryResponse(
        Long id,
        String name,
        int durationMinutes,
        String status,
        int questionCount,
        int answeredCount,
        Instant startedAt,
        Instant expiresAt,
        Instant submittedAt,
        Integer score,
        Integer totalScore
) {
    public static ExamSummaryResponse from(ExamSession session) {
        int answeredCount = (int) session.answers().values().stream()
                .map(ExamAnswer::submittedAnswer)
                .filter(answer -> answer != null && !answer.isBlank())
                .count();
        return new ExamSummaryResponse(
                session.id(),
                session.name(),
                session.durationMinutes(),
                session.status().name(),
                session.questions().size(),
                answeredCount,
                session.startedAt(),
                session.expiresAt(),
                session.submittedAt(),
                session.score(),
                session.totalScore()
        );
    }
}
