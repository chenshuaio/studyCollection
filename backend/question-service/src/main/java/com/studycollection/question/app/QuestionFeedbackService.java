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

    public List<QuestionFeedback> byUser(Long userId) {
        return feedbacks.values().stream()
                .filter(feedback -> feedback.userId().equals(userId))
                .toList();
    }

    public QuestionRevision accept(Long feedbackId, Long adminUserId, String changeSummary, String reviewNote) {
        QuestionFeedback feedback = find(feedbackId);
        updateStatus(feedback, FeedbackStatus.ACCEPTED);
        return new QuestionRevision(
                revisionIds.getAndIncrement(),
                feedback.questionId(),
                feedback.id(),
                adminUserId,
                changeSummary,
                reviewNote
        );
    }

    public QuestionFeedback reject(Long feedbackId, Long adminUserId, String reviewNote) {
        return review(feedbackId, adminUserId, reviewNote, FeedbackStatus.REJECTED);
    }

    public QuestionFeedback markNeedsReview(Long feedbackId, Long adminUserId, String reviewNote) {
        return review(feedbackId, adminUserId, reviewNote, FeedbackStatus.NEEDS_REVIEW);
    }

    private QuestionFeedback review(Long feedbackId, Long adminUserId, String reviewNote, FeedbackStatus status) {
        QuestionFeedback feedback = find(feedbackId);
        if (adminUserId == null) {
            throw new IllegalArgumentException("管理员用户不能为空");
        }
        if (reviewNote == null || reviewNote.isBlank()) {
            throw new IllegalArgumentException("审核备注不能为空");
        }
        return updateStatus(feedback, status);
    }

    private QuestionFeedback updateStatus(QuestionFeedback feedback, FeedbackStatus status) {
        QuestionFeedback reviewed = new QuestionFeedback(
                feedback.id(),
                feedback.userId(),
                feedback.questionId(),
                feedback.type(),
                feedback.content(),
                status
        );
        feedbacks.put(feedback.id(), reviewed);
        return reviewed;
    }
}
