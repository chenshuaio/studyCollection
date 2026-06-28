package com.studycollection.question.app;

import com.studycollection.question.domain.PendingQuestion;
import com.studycollection.question.domain.PendingQuestionStatus;

import java.util.List;

public interface PendingQuestionRepository {
    PendingQuestion save(PendingQuestion question);

    PendingQuestion find(Long id);

    List<PendingQuestion> findByStatus(PendingQuestionStatus status);
}
