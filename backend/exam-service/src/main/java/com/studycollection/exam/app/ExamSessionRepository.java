package com.studycollection.exam.app;

import com.studycollection.exam.domain.ExamSession;

import java.util.List;

public interface ExamSessionRepository {
    ExamSession create(ExamSession session);

    ExamSession save(ExamSession session);

    ExamSession findById(Long id);

    List<ExamSession> findByUserId(Long userId);
}
