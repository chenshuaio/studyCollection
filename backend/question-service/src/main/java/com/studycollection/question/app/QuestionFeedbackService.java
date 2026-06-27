package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.FeedbackType;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class QuestionFeedbackService {
    private final AtomicLong feedbackIds = new AtomicLong(1);
    private final AtomicLong revisionIds = new AtomicLong(1);
    private final Map<Long, QuestionFeedback> feedbacks = new HashMap<>();

    public QuestionFeedback submit(Long userId, Long questionId, FeedbackType type, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        Long id = feedbackIds.getAndIncrement();
        QuestionFeedback feedback = new QuestionFeedback(id, userId, questionId, type, content, FeedbackStatus.PENDING);
        feedbacks.put(id, feedback);
        return feedback;
    }

    public QuestionFeedback find(Long feedbackId) {
        QuestionFeedback feedback = feedbacks.get(feedbackId);
        if (feedback == null) {
            throw new IllegalArgumentException("反馈不存在");
        }
        return feedback;
    }

    public List<QuestionFeedback> pending() {
        return feedbacks.values().stream()
                .filter(feedback -> feedback.status() == FeedbackStatus.PENDING)
                .toList();
    }

    public QuestionRevision accept(Long feedbackId, Long adminUserId, String changeSummary, String reviewNote) {
        QuestionFeedback feedback = find(feedbackId);
        QuestionFeedback accepted = new QuestionFeedback(
                feedback.id(),
                feedback.userId(),
                feedback.questionId(),
                feedback.type(),
                feedback.content(),
                FeedbackStatus.ACCEPTED
        );
        feedbacks.put(feedbackId, accepted);
        return new QuestionRevision(
                revisionIds.getAndIncrement(),
                feedback.questionId(),
                feedback.id(),
                adminUserId,
                changeSummary,
                reviewNote
        );
    }
}
