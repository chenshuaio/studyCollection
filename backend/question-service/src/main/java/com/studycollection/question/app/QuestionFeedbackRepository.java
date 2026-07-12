package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;

import java.util.List;
import java.util.Set;

public interface QuestionFeedbackRepository {
    QuestionFeedback saveFeedback(QuestionFeedback feedback);

    QuestionFeedback findFeedback(Long feedbackId);

    List<QuestionFeedback> findByStatus(FeedbackStatus status);

    List<QuestionFeedback> findByStatuses(Set<FeedbackStatus> statuses);

    List<QuestionFeedback> findByUserId(Long userId);

    QuestionRevision saveRevision(QuestionRevision revision);

    List<QuestionRevision> findRevisionsByQuestionId(Long questionId);

    Set<Long> findScoringAffectedQuestionIds();
}
