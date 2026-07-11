package com.studycollection.question.app;

import com.studycollection.question.domain.FeedbackStatus;
import com.studycollection.question.domain.QuestionFeedback;
import com.studycollection.question.domain.QuestionRevision;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Profile("!local-mysql")
public class InMemoryQuestionFeedbackRepository implements QuestionFeedbackRepository {
    private final AtomicLong feedbackIds = new AtomicLong(1);
    private final AtomicLong revisionIds = new AtomicLong(1);
    private final Map<Long, QuestionFeedback> feedbacks = new LinkedHashMap<>();
    private final Map<Long, QuestionRevision> revisions = new LinkedHashMap<>();

    @Override
    public synchronized QuestionFeedback saveFeedback(QuestionFeedback feedback) {
        Long id = feedback.id() == null ? feedbackIds.getAndIncrement() : feedback.id();
        QuestionFeedback saved = new QuestionFeedback(
                id,
                feedback.userId(),
                feedback.questionId(),
                feedback.type(),
                feedback.content(),
                feedback.status()
        );
        feedbacks.put(id, saved);
        return saved;
    }

    @Override
    public synchronized QuestionFeedback findFeedback(Long feedbackId) {
        QuestionFeedback feedback = feedbacks.get(feedbackId);
        if (feedback == null) {
            throw new IllegalArgumentException("反馈不存在");
        }
        return feedback;
    }

    @Override
    public synchronized List<QuestionFeedback> findByStatus(FeedbackStatus status) {
        return feedbacks.values().stream()
                .filter(feedback -> feedback.status() == status)
                .toList();
    }

    @Override
    public synchronized List<QuestionFeedback> findByUserId(Long userId) {
        return feedbacks.values().stream()
                .filter(feedback -> feedback.userId().equals(userId))
                .toList();
    }

    @Override
    public synchronized QuestionRevision saveRevision(QuestionRevision revision) {
        Long id = revision.id() == null ? revisionIds.getAndIncrement() : revision.id();
        QuestionRevision saved = new QuestionRevision(
                id,
                revision.questionId(),
                revision.feedbackId(),
                revision.adminUserId(),
                revision.changeSummary(),
                revision.reviewNote()
        );
        revisions.put(id, saved);
        return saved;
    }
}
