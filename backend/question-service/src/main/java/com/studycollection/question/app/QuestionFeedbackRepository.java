package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;

import java.util.List;

public interface QuestionFeedbackRepository {
    QuestionFeedback saveFeedback(QuestionFeedback feedback);

    QuestionFeedback findFeedback(Long feedbackId);

    List<QuestionFeedback> findByStatus(FeedbackStatus status);

    List<QuestionFeedback> findByUserId(Long userId);

    QuestionRevision saveRevision(QuestionRevision revision);
}
